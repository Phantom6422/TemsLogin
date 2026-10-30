/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common;

import co.aikar.commands.CommandIssuer;
import co.aikar.commands.CommandManager;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.Nullable;
import xyz.kyngs.librelogin.api.BiHolder;
import xyz.kyngs.librelogin.api.LibreLoginPlugin;
import xyz.kyngs.librelogin.api.Logger;
import xyz.kyngs.librelogin.api.PlatformHandle;
import xyz.kyngs.librelogin.api.configuration.CorruptedConfigurationException;
import xyz.kyngs.librelogin.api.crypto.CryptoProvider;
import xyz.kyngs.librelogin.api.crypto.HashedPassword;
import xyz.kyngs.librelogin.api.database.*;
import xyz.kyngs.librelogin.api.database.connector.DatabaseConnector;
import xyz.kyngs.librelogin.api.database.connector.MySQLDatabaseConnector;
import xyz.kyngs.librelogin.api.database.connector.PostgreSQLDatabaseConnector;
import xyz.kyngs.librelogin.api.database.connector.SQLiteDatabaseConnector;
import xyz.kyngs.librelogin.api.integration.LimboIntegration;
import xyz.kyngs.librelogin.api.premium.PremiumException;
import xyz.kyngs.librelogin.api.premium.PremiumUser;
import xyz.kyngs.librelogin.api.server.ServerHandler;
import xyz.kyngs.librelogin.api.totp.TOTPProvider;
import xyz.kyngs.librelogin.api.util.Release;
import xyz.kyngs.librelogin.api.util.SemanticVersion;
import xyz.kyngs.librelogin.api.util.ThrowableFunction;
import xyz.kyngs.librelogin.common.authorization.AuthenticAuthorizationProvider;
import xyz.kyngs.librelogin.common.command.CommandProvider;
import xyz.kyngs.librelogin.common.command.InvalidCommandArgument;
import xyz.kyngs.librelogin.common.config.HoconMessages;
import xyz.kyngs.librelogin.common.config.HoconPluginConfiguration;
import xyz.kyngs.librelogin.common.crypto.Argon2IDCryptoProvider;
import xyz.kyngs.librelogin.common.crypto.BCrypt2ACryptoProvider;
import xyz.kyngs.librelogin.common.crypto.LogITMessageDigestCryptoProvider;
import xyz.kyngs.librelogin.common.crypto.MessageDigestCryptoProvider;
import xyz.kyngs.librelogin.common.database.AuthenticDatabaseProvider;
import xyz.kyngs.librelogin.common.database.AuthenticUser;
import xyz.kyngs.librelogin.common.database.connector.AuthenticMySQLDatabaseConnector;
import xyz.kyngs.librelogin.common.database.connector.AuthenticPostgreSQLDatabaseConnector;
import xyz.kyngs.librelogin.common.database.connector.AuthenticSQLiteDatabaseConnector;
import xyz.kyngs.librelogin.common.database.connector.DatabaseConnectorRegistration;
import xyz.kyngs.librelogin.common.database.provider.LibreLoginMySQLDatabaseProvider;
import xyz.kyngs.librelogin.common.database.provider.LibreLoginPostgreSQLDatabaseProvider;
import xyz.kyngs.librelogin.common.database.provider.LibreLoginSQLiteDatabaseProvider;
import xyz.kyngs.librelogin.common.event.AuthenticEventProvider;
import xyz.kyngs.librelogin.common.image.AuthenticImageProjector;
import xyz.kyngs.librelogin.common.integration.FloodgateIntegration;
import xyz.kyngs.librelogin.common.integration.luckperms.LuckPermsIntegration;
import xyz.kyngs.librelogin.common.listener.LoginTryListener;
import xyz.kyngs.librelogin.common.log.Log4JFilter;
import xyz.kyngs.librelogin.common.log.LogFilter;
import xyz.kyngs.librelogin.common.mail.AuthenticEMailHandler;
import xyz.kyngs.librelogin.common.migrate.*;
import xyz.kyngs.librelogin.common.premium.AuthenticPremiumProvider;
import xyz.kyngs.librelogin.common.server.AuthenticServerHandler;
import xyz.kyngs.librelogin.common.totp.AuthenticTOTPProvider;
import xyz.kyngs.librelogin.common.util.CancellableTask;
import xyz.kyngs.librelogin.common.util.GeneralUtil;
import xyz.kyngs.librelogin.common.util.LongHashSet;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import static xyz.kyngs.librelogin.common.config.ConfigurationKeys.*;

public abstract class AuthenticLibreLogin<P, S> implements LibreLoginPlugin<P, S> {

    public static final Gson GSON = new Gson();
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd. MM. yyyy HH:mm");
    /**
     * BCrypt only uses the first 72 bytes of a password, so longer passwords are rejected instead of being
     * silently truncated (which would make different passwords equivalent).
     */
    public static final int MAX_PASSWORD_BYTES = 72;
    private static final long MAX_FORBIDDEN_PASSWORDS_SIZE = 64L * 1024 * 1024;
    public static final ExecutorService EXECUTOR;

    static {
        EXECUTOR = new ForkJoinPool(4);
    }

    private final Map<String, CryptoProvider> cryptoProviders;
    private final Map<String, ReadDatabaseProviderRegistration<?, ?, ?>> readProviders;
    private final Map<Class<?>, DatabaseConnectorRegistration<?, ?>> databaseConnectors;
    private final Multimap<P, CancellableTask> cancelOnExit;
    private final List<CancellableTask> persistentTasks;
    private final PlatformHandle<P, S> platformHandle;
    private final LongHashSet forbiddenPasswords;
    protected Logger logger;
    private AuthenticPremiumProvider premiumProvider;
    private AuthenticEventProvider<P, S> eventProvider;
    private AuthenticServerHandler<P, S> serverHandler;
    private TOTPProvider totpProvider;
    private AuthenticImageProjector<P, S> imageProjector;
    private FloodgateIntegration floodgateApi;
    private LuckPermsIntegration<P, S> luckpermsApi;
    private SemanticVersion version;
    private HoconPluginConfiguration configuration;
    private HoconMessages messages;
    private AuthenticAuthorizationProvider<P, S> authorizationProvider;
    private CommandProvider<P, S> commandProvider;
    private ReadWriteDatabaseProvider databaseProvider;
    private DatabaseConnector<?, ?> databaseConnector;
    private AuthenticEMailHandler eMailHandler;
    private LoginTryListener<P, S> loginTryListener;
    private LogFilter logFilter;

    protected AuthenticLibreLogin() {
        cryptoProviders = new ConcurrentHashMap<>();
        readProviders = new ConcurrentHashMap<>();
        databaseConnectors = new ConcurrentHashMap<>();
        platformHandle = providePlatformHandle();
        // The list has close to a million entries; 64-bit hashes need ~8 MB instead of >100 MB of strings.
        forbiddenPasswords = new LongHashSet(1 << 20);
        cancelOnExit = HashMultimap.create();
        persistentTasks = new CopyOnWriteArrayList<>();
    }

    public Map<Class<?>, DatabaseConnectorRegistration<?, ?>> getDatabaseConnectors() {
        return databaseConnectors;
    }

    @Override
    public <E extends Exception, C extends DatabaseConnector<E, ?>> void registerDatabaseConnector(Class<?> clazz, ThrowableFunction<String, C, E> factory, String id) {
        registerDatabaseConnector(new DatabaseConnectorRegistration<>(factory, null, id), clazz);
    }

    @Override
    public void registerReadProvider(ReadDatabaseProviderRegistration<?, ?, ?> registration) {
        readProviders.put(registration.id(), registration);
    }

    @Override
    public AuthenticEMailHandler getEmailHandler() {
        return eMailHandler;
    }

    @Nullable
    @Override
    public LimboIntegration<S> getLimboIntegration() {
        return null;
    }

    @Override
    public User createUser(UUID uuid, UUID premiumUUID, HashedPassword hashedPassword, String lastNickname, Timestamp joinDate, Timestamp lastSeen, String secret, String ip, Timestamp lastAuthentication, String lastServer, String email) {
        return new AuthenticUser(uuid, premiumUUID, hashedPassword, lastNickname, joinDate, lastSeen, secret, ip, lastAuthentication, lastServer, email);
    }

    public void registerDatabaseConnector(DatabaseConnectorRegistration<?, ?> registration, Class<?> clazz) {
        databaseConnectors.put(clazz, registration);
    }

    @Override
    public PlatformHandle<P, S> getPlatformHandle() {
        return platformHandle;
    }

    protected abstract PlatformHandle<P, S> providePlatformHandle();

    @Override
    public SemanticVersion getParsedVersion() {
        return version;
    }


    @Override
    public boolean validPassword(String password) {
        if (password == null) {
            return false;
        }

        if (password.length() < configuration.get(MINIMUM_PASSWORD_LENGTH)) {
            return false;
        }

        // BCrypt silently ignores everything after 72 bytes, so accepting longer passwords would make
        // two different passwords authenticate the same account. Rejecting them keeps hashes unambiguous.
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return false;
        }

        return !forbiddenPasswords.contains(hashPassword(password.toUpperCase(Locale.ROOT)));
    }

    @Override
    public Map<String, ReadDatabaseProviderRegistration<?, ?, ?>> getReadProviders() {
        return Map.copyOf(readProviders);
    }

    public CommandProvider<P, S> getCommandProvider() {
        return commandProvider;
    }

    @Override
    public ReadWriteDatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }

    @Override
    public AuthenticPremiumProvider getPremiumProvider() {
        return premiumProvider;
    }

    @Override
    public TOTPProvider getTOTPProvider() {
        return totpProvider;
    }

    @Override
    public AuthenticImageProjector<P, S> getImageProjector() {
        return imageProjector;
    }

    @Override
    public ServerHandler<P, S> getServerHandler() {
        return serverHandler;
    }

    protected void enable() {
        version = SemanticVersion.parse(getVersion());
        if (logger == null) logger = provideLogger();

        try {
            logFilter = new Log4JFilter();
            logFilter.inject();
        } catch (Throwable ignored) {
            logFilter = null;
            logger.warn("LogFilter is not supported on this platform, are you not running Log4J?");
        }

        var folder = getDataFolder();

        // Import configuration, messages and databases from a LibreLogin installation before anything is loaded.
        LegacyDataMigrator.migrate(folder.getParentFile(), folder, logger);

        try (var license = getResourceAsStream("LICENSE.txt")) {
            if (license != null) {
                Files.copy(license, new File(folder, "LICENSE.txt").toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                logger.warn("Bundled LICENSE.txt is missing, the plugin is still licensed under the MPL-2.0");
            }
        } catch (IOException ignored) {
            // Silently ignore
        }

        if (platformHandle.getPlatformIdentifier().equals("paper")) {
            LIMBO.setDefault(List.of("limbo"));

            var lobby = HashMultimap.<String, String>create();
            lobby.put("root", "world");

            LOBBY.setDefault(lobby);
        }

        eventProvider = new AuthenticEventProvider<>(this);
        premiumProvider = new AuthenticPremiumProvider(this);

        registerCryptoProvider(new MessageDigestCryptoProvider("SHA-256"));
        registerCryptoProvider(new MessageDigestCryptoProvider("SHA-512"));
        registerCryptoProvider(new BCrypt2ACryptoProvider());
        registerCryptoProvider(new Argon2IDCryptoProvider(logger));
        registerCryptoProvider(new LogITMessageDigestCryptoProvider("LOGIT-SHA-256", "SHA-256"));

        setupDB();

        checkDataFolder();

        loadConfigs();

        logger.info("Loading forbidden passwords...");

        try {
            loadForbiddenPasswords();
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("An unknown exception occurred while attempting to load the forbidden passwords, this most likely isn't your fault");
            shutdownProxy(1);
        }

        logger.info("Loaded %s forbidden passwords".formatted(forbiddenPasswords.size()));

        connectToDB();

        serverHandler = new AuthenticServerHandler<>(this);

        this.loginTryListener = new LoginTryListener<>(this);

        // Moved to a different class to avoid class loading issues
        GeneralUtil.checkAndMigrate(configuration, logger, this);

        imageProjector = provideImageProjector();

        if (imageProjector != null) {
            if (!configuration.get(TOTP_ENABLED)) {
                imageProjector = null;
                logger.warn("2FA is disabled in the configuration, aborting...");
            } else {
                imageProjector.enable();
            }
        }

        totpProvider = imageProjector == null ? null : new AuthenticTOTPProvider(this);
        eMailHandler = configuration.get(MAIL_ENABLED) ? new AuthenticEMailHandler(this) : null;

        authorizationProvider = new AuthenticAuthorizationProvider<>(this);
        commandProvider = new CommandProvider<>(this);

        if (version.dev()) {
            logger.warn("!! YOU ARE RUNNING A DEVELOPMENT BUILD OF TEMSLOGIN !!");
            logger.warn("!! THIS IS NOT A RELEASE, USE THIS ONLY IF YOU WERE INSTRUCTED TO DO SO. DO NOT USE THIS IN PRODUCTION !!");
        } else {
            initMetrics();
        }

        delay(this::checkForUpdates, 1000);

        if (pluginPresent("floodgate")) {
            logger.info("Floodgate detected, enabling bedrock support...");
            floodgateApi = new FloodgateIntegration();
        }

        if (pluginPresent("luckperms")) {
            logger.info("LuckPerms detected, enabling context provider");
            luckpermsApi = new LuckPermsIntegration<>(this);
        }

        if (multiProxyEnabled()) {
            logger.info("Detected MultiProxy setup, enabling MultiProxy support...");
        }
    }


    public <C extends DatabaseConnector<?, ?>> DatabaseConnectorRegistration<?, C> getDatabaseConnector(Class<C> clazz) {
        return (DatabaseConnectorRegistration<?, C>) databaseConnectors.get(clazz);
    }

    private void connectToDB() {
        logger.info("Connecting to the database...");

        try {
            var registration = readProviders.get(configuration.get(DATABASE_TYPE));
            if (registration == null) {
                logger.error("Database type %s doesn't exist, please check your configuration".formatted(configuration.get(DATABASE_TYPE)));
                shutdownProxy(1);
            }

            DatabaseConnector<?, ?> connector = null;

            if (registration.databaseConnector() != null) {
                var connectorRegistration = getDatabaseConnector(registration.databaseConnector());

                if (connectorRegistration == null) {
                    logger.error("Database type %s is corrupted, please use a different one".formatted(configuration.get(DATABASE_TYPE)));
                    shutdownProxy(1);
                }

                connector = connectorRegistration.factory().apply("database.properties." + connectorRegistration.id() + ".");

                connector.connect();
            }

            var provider = registration.create(connector);

            if (provider instanceof ReadWriteDatabaseProvider casted) {
                databaseProvider = casted;
                databaseConnector = connector;
            } else {
                logger.error("Database type %s cannot be used for writing, please use a different one".formatted(configuration.get(DATABASE_TYPE)));
                shutdownProxy(1);
            }

        } catch (Exception e) {
            var cause = GeneralUtil.getFurthestCause(e);
            logger.error("!! THIS IS MOST LIKELY NOT AN ERROR CAUSED BY TEMSLOGIN !!");
            logger.error("Failed to connect to the database, this most likely is caused by wrong credentials. Cause: %s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage()));
            shutdownProxy(1);
        }

        logger.info("Successfully connected to the database");

        if (databaseProvider instanceof AuthenticDatabaseProvider<?> casted) {
            logger.info("Validating schema");

            try {
                casted.validateSchema();
            } catch (Exception e) {
                var cause = GeneralUtil.getFurthestCause(e);
                logger.error("Failed to validate schema! Cause: %s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage()));
                logger.error("Please open an issue on our GitHub, or visit Discord support");
                shutdownProxy(1);
            }

            logger.info("Schema validated");
        }
    }

    private void loadConfigs() {
        logger.info("Loading messages...");

        messages = new HoconMessages(logger);

        try {
            messages.reload(this);
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("An unknown exception occurred while attempting to load the messages, this most likely isn't your fault");
            shutdownProxy(1);
        } catch (CorruptedConfigurationException e) {
            var cause = GeneralUtil.getFurthestCause(e);
            logger.error("!! THIS IS MOST LIKELY NOT AN ERROR CAUSED BY TEMSLOGIN !!");
            logger.error("!!The messages are corrupted, please look below for further clues. If you are clueless, delete the messages and a new ones will be created for you. Cause: %s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage()));
            shutdownProxy(1);
        }

        logger.info("Loading configuration...");

        var defaults = new ArrayList<BiHolder<Class<?>, String>>();

        for (DatabaseConnectorRegistration<?, ?> value : databaseConnectors.values()) {
            if (value.configClass() == null) continue;
            defaults.add(new BiHolder<>(value.configClass(), "database.properties." + value.id() + "."));
            defaults.add(new BiHolder<>(value.configClass(), "migration.old-database." + value.id() + "."));
        }

        configuration = new HoconPluginConfiguration(logger, defaults);

        try {
            if (configuration.reload(this)) {
                logger.warn("!! A new configuration was generated, please fill it out, if in doubt, see the wiki !!");
                shutdownProxy(0);
            }

            var limbos = configuration.get(LIMBO);
            var lobby = configuration.get(LOBBY);

            for (String value : lobby.values()) {
                if (limbos.contains(value)) {
                    throw new CorruptedConfigurationException("Lobby server/world %s is also a limbo server/world, this is not allowed".formatted(value));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("An unknown exception occurred while attempting to load the configuration, this most likely isn't your fault");
            shutdownProxy(1);
        } catch (CorruptedConfigurationException e) {
            var cause = GeneralUtil.getFurthestCause(e);
            logger.error("!! THIS IS MOST LIKELY NOT AN ERROR CAUSED BY TEMSLOGIN !!");
            logger.error("!!The configuration is corrupted, please look below for further clues. If you are clueless, delete the config and a new one will be created for you. Cause: %s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage()));
            shutdownProxy(1);
        }
    }

    private void setupDB() {
        registerDatabaseConnector(new DatabaseConnectorRegistration<>(
                        prefix -> new AuthenticMySQLDatabaseConnector(this, prefix),
                        AuthenticMySQLDatabaseConnector.Configuration.class,
                        "mysql"
                ),
                MySQLDatabaseConnector.class);
        registerDatabaseConnector(new DatabaseConnectorRegistration<>(
                        prefix -> new AuthenticSQLiteDatabaseConnector(this, prefix),
                        AuthenticSQLiteDatabaseConnector.Configuration.class,
                        "sqlite"
                ),
                SQLiteDatabaseConnector.class);
        registerDatabaseConnector(new DatabaseConnectorRegistration<>(
                        prefix -> new AuthenticPostgreSQLDatabaseConnector(this, prefix),
                        AuthenticPostgreSQLDatabaseConnector.Configuration.class,
                        "postgresql"
                ),
                PostgreSQLDatabaseConnector.class);

        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new LibreLoginMySQLDatabaseProvider(connector, this),
                "librelogin-mysql",
                MySQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new LibreLoginSQLiteDatabaseProvider(connector, this),
                "librelogin-sqlite",
                SQLiteDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new LibreLoginPostgreSQLDatabaseProvider(connector, this),
                "librelogin-postgresql",
                PostgreSQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new AegisSQLMigrateReadProvider(configuration.get(MIGRATION_MYSQL_OLD_DATABASE_TABLE), logger, connector),
                "aegis-mysql",
                MySQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new AuthMeSQLMigrateReadProvider(configuration.get(MIGRATION_MYSQL_OLD_DATABASE_TABLE), logger, connector),
                "authme-mysql",
                MySQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new AuthMeSQLMigrateReadProvider(configuration.get(MIGRATION_POSTGRESQL_OLD_DATABASE_TABLE), logger, connector),
                "authme-postgresql",
                PostgreSQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new AuthMeSQLMigrateReadProvider("authme", logger, connector),
                "authme-sqlite",
                SQLiteDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new DBASQLMigrateReadProvider(configuration.get(MIGRATION_MYSQL_OLD_DATABASE_TABLE), logger, connector),
                "dba-mysql",
                MySQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new JPremiumSQLMigrateReadProvider(configuration.get(MIGRATION_MYSQL_OLD_DATABASE_TABLE), logger, connector),
                "jpremium-mysql",
                MySQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new NLoginSQLMigrateReadProvider("nlogin", logger, connector),
                "nlogin-sqlite",
                SQLiteDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new NLoginSQLMigrateReadProvider("nlogin", logger, connector),
                "nlogin-mysql",
                MySQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new FastLoginSQLMigrateReadProvider("premium", logger, connector, databaseConnector, premiumProvider),
                "fastlogin-mysql",
                MySQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new FastLoginSQLMigrateReadProvider("premium", logger, connector, databaseConnector, premiumProvider),
                "fastlogin-sqlite",
                SQLiteDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new UniqueCodeAuthSQLMigrateReadProvider("uniquecode_proxy_users", logger, connector, this),
                "uniquecodeauth-mysql",
                MySQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new LoginSecuritySQLMigrateReadProvider("ls_players", logger, connector),
                "loginsecurity-mysql",
                MySQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new LoginSecuritySQLMigrateReadProvider("ls_players", logger, connector),
                "loginsecurity-sqlite",
                SQLiteDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new LimboAuthSQLMigrateReadProvider("AUTH", logger, connector),
                "limboauth-mysql",
                MySQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new AuthySQLMigrateReadProvider("players", logger, connector),
                "authy-mysql",
                MySQLDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new AuthySQLMigrateReadProvider("players", logger, connector),
                "authy-sqlite",
                SQLiteDatabaseConnector.class
        ));
        registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new LogItSQLMigrateReadProvider(configuration.get(MIGRATION_MYSQL_OLD_DATABASE_TABLE), logger, connector),
                "logit-mysql",
                MySQLDatabaseConnector.class
        ));
        // Currently disabled as crazylogin stores all names in lowercase
        /*registerReadProvider(new ReadDatabaseProviderRegistration<>(
                connector -> new CrazyLoginSQLMigrateReadProvider(configuration.get(MIGRATION_MYSQL_OLD_DATABASE_TABLE), logger, connector),
                "crazylogin-mysql",
                MySQLDatabaseConnector.class
        ));*/
    }

    private void loadForbiddenPasswords() throws IOException {
        var file = new File(getDataFolder(), "forbidden-passwords.txt");

        if (!file.exists()) {
            logger.info("Forbidden passwords list doesn't exist, downloading...");

            // URL#openStream() has no timeouts, so a stalled endpoint would block plugin startup forever.
            var connection = URI.create("https://raw.githubusercontent.com/Phantom6422/TemsLogin/dev/forbidden-passwords.txt").toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "TemsLogin");

            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream())) {
                if (!file.createNewFile()) {
                    throw new IOException("Failed to create file");
                }
                try (var fos = new FileOutputStream(file)) {
                    var dataBuffer = new byte[1024];
                    long total = 0;
                    int bytesRead;
                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        total += bytesRead;

                        // The real list is a few megabytes. Refusing oversized downloads prevents a
                        // misbehaving or compromised endpoint from filling the disk.
                        if (total > MAX_FORBIDDEN_PASSWORDS_SIZE) {
                            throw new IOException("Forbidden passwords list is larger than the allowed maximum");
                        }

                        fos.write(dataBuffer, 0, bytesRead);
                    }
                }
                logger.info("Successfully downloaded forbidden passwords list");
            } catch (IOException e) {
                e.printStackTrace();
                logger.warn("Failed to download forbidden passwords list, using template instead");

                // Do not leave a partially downloaded file behind, it would be treated as complete next start.
                if (file.exists() && !file.delete()) {
                    logger.warn("Failed to delete the partial forbidden passwords list");
                }

                try (var template = getResourceAsStream("forbidden-passwords-template.txt")) {
                    if (template == null) {
                        throw new IOException("The bundled forbidden passwords template is missing");
                    }
                    Files.copy(template, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                if (connection instanceof HttpURLConnection http) {
                    http.disconnect();
                }
            }
        }

        try (var reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# ")) {
                    continue;
                }
                forbiddenPasswords.add(hashPassword(line.toUpperCase(Locale.ROOT)));
            }
        }

        forbiddenPasswords.freeze();
    }

    /**
     * Maps a password to the 64 bits stored in the forbidden password set. Truncated SHA-256 keeps the
     * chance of a false positive negligible (roughly 3e-8 for a million entries) and cannot be used to
     * bypass the list: a collision only makes an extra password rejected.
     */
    private static long hashPassword(String password) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            return ByteBuffer.wrap(hash).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private void checkForUpdates() {
        logger.info("Checking for updates...");

        try {
            var connection = URI.create("https://api.github.com/repos/Phantom6422/TemsLogin/releases").toURL().openConnection();

            connection.setRequestProperty("User-Agent", "TemsLogin");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            JsonArray root;

            try (var in = new InputStreamReader(connection.getInputStream())) {
                root = GSON.fromJson(in, JsonArray.class);
            }

            if (root == null) {
                logger.warn("Failed to check for updates: GitHub returned an empty response");
                return;
            }

            List<Release> behind = new ArrayList<>();
            SemanticVersion latest = null;

            for (JsonElement raw : root) {
                if (!raw.isJsonObject()) {
                    continue;
                }

                var release = raw.getAsJsonObject();

                if (isTrue(release, "draft") || isTrue(release, "prerelease")) {
                    continue;
                }

                var tag = release.get("tag_name");

                if (tag == null || tag.isJsonNull()) {
                    continue;
                }

                var tagName = tag.getAsString();
                SemanticVersion version;

                try {
                    version = SemanticVersion.parse(tagName);
                } catch (RuntimeException e) {
                    logger.warn("Failed to parse version tag '%s', skipping it".formatted(tagName));
                    continue;
                }

                if (latest == null || version.compare(latest) > 0) {
                    latest = version;
                }

                if (this.version.compare(version) >= 0) {
                    continue;
                }

                var name = release.get("name");

                behind.add(new Release(version, name == null || name.isJsonNull() ? tagName : name.getAsString()));
            }

            if (latest == null) {
                logger.warn("Failed to check for updates: no valid releases found");
                return;
            }

            if (behind.isEmpty()) {
                logger.info("You are running the latest version of TemsLogin");
            } else {
                behind.sort(Comparator.comparing(Release::version));
                logger.warn("!! YOU ARE RUNNING AN OUTDATED VERSION OF TEMSLOGIN !!");
                logger.info("You are running version %s, the latest version is %s. You are running %s versions behind. Newer versions:".formatted(getVersion(), latest, behind.size()));
                for (Release release : behind) {
                    logger.info("- %s".formatted(release.name()));
                }
                logger.warn("!! PLEASE UPDATE TO THE LATEST VERSION !!");
            }
        } catch (Exception e) {
            logger.warn("Failed to check for updates", e);
        }
    }

    private static boolean isTrue(JsonObject object, String member) {
        var element = object.get(member);

        return element != null && !element.isJsonNull() && element.getAsBoolean();
    }

    public UUID generateNewUUID(String name, @Nullable UUID premiumID) {
        return switch (configuration.getNewUUIDCreator()) {
            case RANDOM -> UUID.randomUUID();
            case MOJANG -> premiumID == null ? GeneralUtil.getCrackedUUIDFromName(name) : premiumID;
            case CRACKED -> GeneralUtil.getCrackedUUIDFromName(name);
        };
    }

    protected void disable() {
        persistentTasks.forEach(CancellableTask::cancel);
        persistentTasks.clear();

        if (logFilter != null) {
            try {
                logFilter.remove();
            } catch (Throwable ignored) {
                // The platform may not support Log4J, nothing to clean up in that case.
            }
            logFilter = null;
        }

        if (databaseConnector != null) {
            try {
                databaseConnector.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Failed to disconnect from database, ignoring...");
            }
        }
        if (luckpermsApi != null) {
            luckpermsApi.disable();
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public HoconPluginConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public HoconMessages getMessages() {
        return messages;
    }

    @Override
    public void checkDataFolder() {
        var folder = getDataFolder();

        if (!folder.exists()) if (!folder.mkdir()) throw new RuntimeException("Failed to create datafolder");
    }

    protected abstract Logger provideLogger();

    public abstract CommandManager<?, ?, ?, ?, ?, ?> provideManager();

    public abstract P getPlayerFromIssuer(CommandIssuer issuer);

    public abstract void authorize(P player, User user, Audience audience);

    public abstract CancellableTask delay(Runnable runnable, long delayInMillis);

    public abstract CancellableTask repeat(Runnable runnable, long delayInMillis, long repeatInMillis);

    public abstract boolean pluginPresent(String pluginName);

    protected abstract AuthenticImageProjector<P, S> provideImageProjector();

    public PremiumUser getUserOrThrowICA(String username) throws InvalidCommandArgument {
        try {
            return getPremiumProvider().getUserForName(username);
        } catch (PremiumException e) {
            throw new InvalidCommandArgument(getMessages().getMessage(
                    switch (e.getIssue()) {
                        case THROTTLED -> "error-premium-throttled";
                        default -> "error-premium-unknown";
                    }
            ));
        }
    }

    /**
     * Telemetry is deliberately disabled in this fork: the upstream bStats project ids belong to LibreLogin
     * and reporting there would be misleading. Override this if you register your own bStats project.
     */
    protected void initMetrics() {
        // No-op by default.
    }

    @Override
    public AuthenticAuthorizationProvider<P, S> getAuthorizationProvider() {
        return authorizationProvider;
    }

    @Override
    public CryptoProvider getCryptoProvider(String id) {
        return cryptoProviders.get(id);
    }

    @Override
    public void registerCryptoProvider(CryptoProvider provider) {
        cryptoProviders.put(provider.getIdentifier(), provider);
    }

    @Override
    public CryptoProvider getDefaultCryptoProvider() {
        return getCryptoProvider(configuration.get(DEFAULT_CRYPTO_PROVIDER));
    }

    @Override
    public void migrate(ReadDatabaseProvider from, WriteDatabaseProvider to) {
        logger.info("Reading data...");
        var users = from.getAllUsers();
        logger.info("Data read, inserting into database...");
        to.insertUsers(users);
    }

    @Override
    public AuthenticEventProvider<P, S> getEventProvider() {
        return eventProvider;
    }

    public LoginTryListener<P, S> getLoginTryListener() {
        return loginTryListener;
    }

    public void onExit(P player) {
        cancelOnExit.removeAll(player).forEach(CancellableTask::cancel);
        if (configuration.get(REMEMBER_LAST_SERVER)) {
            var server = platformHandle.getPlayersServerName(player);
            if (server == null) return;
            var user = databaseProvider.getByUUID(platformHandle.getUUIDForPlayer(player));
            if (user != null && !getConfiguration().get(LIMBO).contains(server)) {
                user.setLastServer(server);
                databaseProvider.updateUser(user);
            }
        }
    }

    public void cancelOnExit(CancellableTask task, P player) {
        cancelOnExit.put(player, task);
    }

    /**
     * Schedules a repeating task that lives until the plugin is disabled.
     */
    public CancellableTask repeatPersistently(Runnable runnable, long delayInMillis, long repeatInMillis) {
        var task = repeat(runnable, delayInMillis, repeatInMillis);

        persistentTasks.add(task);

        return task;
    }

    public boolean floodgateEnabled() {
        return floodgateApi != null;
    }

    public boolean luckpermsEnabled() {
        return luckpermsApi != null;
    }

    public boolean fromFloodgate(UUID uuid) {
        return floodgateApi != null && uuid != null && floodgateApi.isFloodgateId(uuid);
    }

    protected void shutdownProxy(int code) {
        //noinspection finally
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        } finally {
            System.exit(code);
        }
    }

    public abstract Audience getAudienceFromIssuer(CommandIssuer issuer);

    protected boolean mainThread() {
        return false;
    }

    public void reportMainThread() {
        if (mainThread()) {
            logger.error("AN IO OPERATION IS BEING PERFORMED ON THE MAIN THREAD! THIS IS A SERIOUS BUG!, PLEASE REPORT IT TO THE DEVELOPER OF THE PLUGIN AND ATTACH THE STACKTRACE BELOW!");
            new Throwable().printStackTrace();
        }
    }

    public boolean fromFloodgate(String username) {
        return floodgateApi != null && floodgateApi.getPlayer(username) != null;
    }
}
