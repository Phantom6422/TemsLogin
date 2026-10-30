/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.premium;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonObject;
import xyz.kyngs.librelogin.api.premium.PremiumException;
import xyz.kyngs.librelogin.api.premium.PremiumProvider;
import xyz.kyngs.librelogin.api.premium.PremiumUser;
import xyz.kyngs.librelogin.api.util.ThrowableFunction;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.util.GeneralUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AuthenticPremiumProvider implements PremiumProvider {

    private static final int TIMEOUT_MILLIS = 5000;

    private final Cache<String, PremiumUser> userCache;
    private final List<ThrowableFunction<String, PremiumUser, PremiumException>> fetchers;
    private final AuthenticLibreLogin<?, ?> plugin;

    public AuthenticPremiumProvider(AuthenticLibreLogin<?, ?> plugin) {
        this.plugin = plugin;
        userCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();

        fetchers = new ArrayList<>(3);

        fetchers.add(this::getUserFromMojang);
        fetchers.add(this::getUserFromPlayerDB);
        fetchers.add(this::getUserFromMinetools);
        //fetchers.add(this::getUserFromAshcon); //Momentarily disabled, as it's unreliable. See https://github.com/Electroid/mojang-api/issues/79
    }

    @Override
    public PremiumUser getUserForName(String name) throws PremiumException {
        name = name.toLowerCase(java.util.Locale.ROOT);

        var exceptionToThrow = new PremiumException[1];

        String finalName = name;
        var result = userCache.get(name, x -> {
            for (int i = 0; i < fetchers.size(); i++) {
                var fetcher = fetchers.get(i);

                try {
                    return fetcher.apply(x);
                } catch (PremiumException e) {
                    if (i == fetchers.size() - 1) {
                        exceptionToThrow[0] = e;
                    } else {
                        String reason = switch (e.getIssue()) {
                            case TIMED_OUT -> "API request timed out (" + (e.getMessage() != null ? e.getMessage() : "timeout") + ")";
                            case THROTTLED -> "API rate limit reached (HTTP 429)";
                            case SERVER_EXCEPTION -> "API server error (" + (e.getMessage() != null ? e.getMessage() : "server error") + ")";
                            default -> (e.getMessage() != null ? e.getMessage() : "unexpected error");
                        };
                        plugin.getLogger().warn("Could not fetch premium user info: " + reason + ". Falling back to an alternative API.");
                        plugin.getLogger().debug("Alternative API fallback details", e);
                    }
                } catch (RuntimeException e) {
                    plugin.getLogger().debug("Unexpected exception while fetching premium user " + finalName, e);
                    if (i == fetchers.size() - 1) {
                        exceptionToThrow[0] = new PremiumException(PremiumException.Issue.UNDEFINED, e);
                    }
                }
            }
            return null;
        });

        if (exceptionToThrow[0] != null) {
            throw exceptionToThrow[0];
        }

        return result;
    }

    /**
     * Opens a connection with the timeouts every request needs. Without them a stalled remote API can block
     * the login thread, and keeping the connections open leaks file descriptors until they are collected.
     */
    private HttpURLConnection open(String url) throws IOException {
        plugin.reportMainThread();

        var connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", "TemsLogin");

        return connection;
    }

    private static JsonObject readJson(HttpURLConnection connection) throws IOException {
        try (var reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            return AuthenticLibreLogin.GSON.fromJson(reader, JsonObject.class);
        }
    }

    private PremiumUser getUserFromAshcon(String name) throws PremiumException {
        try {
            var connection = open("https://api.ashcon.app/mojang/v2/user/" + name);

            try {
                switch (connection.getResponseCode()) {
                    case 200 -> {
                        var data = readJson(connection);

                        var uuid = data.get("uuid");
                        var username = data.get("username").getAsString();

                        return new PremiumUser(UUID.fromString(uuid.getAsString()), username, username.equalsIgnoreCase(name));
                    }
                    case 404 -> {
                        return null;
                    }
                    case 429 ->
                            throw new PremiumException(PremiumException.Issue.THROTTLED, GeneralUtil.readInput(connection.getErrorStream()));
                    default ->
                            throw new PremiumException(PremiumException.Issue.UNDEFINED, GeneralUtil.readInput(connection.getErrorStream()));
                }
            } finally {
                connection.disconnect();
            }
        } catch (SocketTimeoutException te) {
            throw new PremiumException(PremiumException.Issue.TIMED_OUT, "Ashcon API timed out");
        } catch (IOException e) {
            throw new PremiumException(PremiumException.Issue.SERVER_EXCEPTION, e);
        }
    }

    private PremiumUser getUserFromPlayerDB(String name) throws PremiumException {
        try {
            var connection = open("https://playerdb.co/api/player/minecraft/" + name);

            try {
                switch (connection.getResponseCode()) {
                    case 200 -> {
                        var data = readJson(connection);

                        var id = data.get("data").getAsJsonObject().get("player").getAsJsonObject().get("id").getAsString();
                        var username = data.get("data").getAsJsonObject().get("player").getAsJsonObject().get("username").getAsString();

                        return new PremiumUser(
                                UUID.fromString(id),
                                username,
                                username.equalsIgnoreCase(name)
                        );
                    }
                    case 400 -> {
                        return null;
                    }
                    case 500 ->
                            throw new PremiumException(PremiumException.Issue.SERVER_EXCEPTION, GeneralUtil.readInput(connection.getErrorStream()));
                    default ->
                            throw new PremiumException(PremiumException.Issue.UNDEFINED, GeneralUtil.readInput(connection.getErrorStream()));
                }
            } finally {
                connection.disconnect();
            }
        } catch (SocketTimeoutException te) {
            throw new PremiumException(PremiumException.Issue.TIMED_OUT, "PlayerDB API timed out");
        } catch (IOException e) {
            throw new PremiumException(PremiumException.Issue.SERVER_EXCEPTION, e);
        }
    }

    private PremiumUser getUserFromMinetools(String name) throws PremiumException {
        try {
            var connection = open("https://api.minetools.eu/uuid/" + name);

            try {
                switch (connection.getResponseCode()) {
                    case 200 -> {
                        var data = readJson(connection);

                        var rawId = data.get("id");
                        if (rawId == null || rawId.isJsonNull()) {
                            var error = data.get("error");
                            if (error == null) {
                                return null;
                            }
                            var errorMessage = error.getAsString();
                            if (errorMessage.equals("Invalid UUID or nickname.")) {
                                return null;
                            } else {
                                throw new PremiumException(PremiumException.Issue.UNDEFINED, errorMessage);
                            }
                        }
                        var username = data.get("name").getAsString();

                        return new PremiumUser(
                                GeneralUtil.fromUnDashedUUID(rawId.getAsString()),
                                username,
                                username.equalsIgnoreCase(name)
                        );
                    }
                    case 400 -> {
                        return null;
                    }
                    case 500 ->
                            throw new PremiumException(PremiumException.Issue.SERVER_EXCEPTION, GeneralUtil.readInput(connection.getErrorStream()));
                    default ->
                            throw new PremiumException(PremiumException.Issue.UNDEFINED, GeneralUtil.readInput(connection.getErrorStream()));
                }
            } finally {
                connection.disconnect();
            }
        } catch (SocketTimeoutException te) {
            throw new PremiumException(PremiumException.Issue.TIMED_OUT, "Minetools API timed out");
        } catch (IOException e) {
            throw new PremiumException(PremiumException.Issue.SERVER_EXCEPTION, e);
        }
    }

    private PremiumUser getUserFromMojang(String name) throws PremiumException {
        try {
            var connection = open("https://api.mojang.com/users/profiles/minecraft/" + name);

            try {
                return switch (connection.getResponseCode()) {
                    case 429 ->
                            throw new PremiumException(PremiumException.Issue.THROTTLED, GeneralUtil.readInput(connection.getErrorStream()));
                    case 204, 404 -> null;
                    case 200 -> {
                        var data = readJson(connection);

                        var id = data.get("id").getAsString();
                        var demo = data.get("demo");

                        yield demo != null ? null : new PremiumUser(
                                GeneralUtil.fromUnDashedUUID(id),
                                data.get("name").getAsString(),
                                true // Mojang API is always authoritative
                        );
                    }
                    case 403 -> {
                        if ("text/html".equals(connection.getContentType())) {
                            throw new PremiumException(PremiumException.Issue.SERVER_EXCEPTION, GeneralUtil.readInput(connection.getErrorStream()));
                        }
                        throw new PremiumException(PremiumException.Issue.UNDEFINED, GeneralUtil.readInput(connection.getErrorStream()));
                    }
                    case 500 ->
                            throw new PremiumException(PremiumException.Issue.SERVER_EXCEPTION, GeneralUtil.readInput(connection.getErrorStream()));
                    default ->
                            throw new PremiumException(PremiumException.Issue.UNDEFINED, GeneralUtil.readInput(connection.getErrorStream()));
                };
            } finally {
                connection.disconnect();
            }
        } catch (SocketTimeoutException te) {
            throw new PremiumException(PremiumException.Issue.TIMED_OUT, "Mojang API timed out");
        } catch (IOException e) {
            throw new PremiumException(PremiumException.Issue.UNDEFINED, e);
        }
    }

    @Override
    public PremiumUser getUserForUUID(UUID uuid) throws PremiumException {
        try {
            var connection = open("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);

            try {
                return switch (connection.getResponseCode()) {
                    case 429 ->
                            throw new PremiumException(PremiumException.Issue.THROTTLED, GeneralUtil.readInput(connection.getErrorStream()));
                    case 204, 404 -> null;
                    case 200 -> {
                        var data = readJson(connection);

                        var name = data.get("name").getAsString();

                        yield new PremiumUser(uuid, name, true); // Mojang API is always authoritative
                    }
                    case 500 ->
                            throw new PremiumException(PremiumException.Issue.SERVER_EXCEPTION, GeneralUtil.readInput(connection.getErrorStream()));
                    default ->
                            throw new PremiumException(PremiumException.Issue.UNDEFINED, GeneralUtil.readInput(connection.getErrorStream()));
                };
            } finally {
                connection.disconnect();
            }
        } catch (SocketTimeoutException te) {
            throw new PremiumException(PremiumException.Issue.TIMED_OUT, "Mojang session API timed out");
        } catch (IOException e) {
            throw new PremiumException(PremiumException.Issue.UNDEFINED, e);
        }
    }
}
