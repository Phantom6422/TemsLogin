/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Compatibility helpers that allow the same jar to run on Paper, Spigot and Bukkit.
 * <p>
 * Paper-only APIs are always accessed through reflection, so the classes in this jar never link
 * against methods that do not exist on Spigot.
 */
public final class PaperUtil {

    private static final boolean PAPER = classExists("io.papermc.paper.plugin.loader.PluginLoader");

    private PaperUtil() {
    }

    /**
     * @return whether the server is running Paper (or a Paper fork such as Purpur)
     */
    public static boolean isPaper() {
        return PAPER;
    }

    public static String getServerBrand() {
        return PAPER ? "Paper" : Bukkit.getName();
    }

    /**
     * Returns an Adventure audience for the given sender. Paper implements {@link Audience} natively,
     * Spigot and Bukkit are wrapped in a legacy serializer.
     */
    public static Audience getAudience(CommandSender sender) {
        if (sender instanceof Audience audience) {
            return audience;
        }

        return new BukkitAudience(sender);
    }

    public static void kick(Player player, Component reason) {
        if (PAPER) {
            try {
                Player.class.getMethod("kick", Component.class).invoke(player, reason);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Fall back to the Bukkit method below.
            }
        }

        player.kickPlayer(LegacyComponentSerializer.legacySection().serialize(reason));
    }

    public static CompletableFuture<Boolean> teleport(PaperLibreLogin plugin, Player player, Location location) {
        if (PAPER) {
            try {
                Method teleportAsync = Player.class.getMethod("teleportAsync", Location.class);
                @SuppressWarnings("unchecked")
                var future = (CompletableFuture<Boolean>) teleportAsync.invoke(player, location);

                if (future != null) {
                    return future;
                }
            } catch (ReflectiveOperationException ignored) {
                // Fall back to the synchronous teleport below.
            }
        }

        if (Bukkit.isPrimaryThread()) {
            return CompletableFuture.completedFuture(player.teleport(location));
        }

        var future = new CompletableFuture<Boolean>();
        Bukkit.getScheduler().runTask(plugin.getBootstrap(), () -> future.complete(player.teleport(location)));
        return future;
    }

    public static void setInvisible(Player player, boolean invisible) {
        if (PAPER) {
            try {
                Player.class.getMethod("setInvisible", boolean.class).invoke(player, invisible);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Fall back to a potion effect below.
            }
        }

        if (invisible) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    /**
     * Checks whether the server is configured to run behind a proxy. Works on both Spigot and Paper,
     * even though the configuration layout differs between them.
     */
    public static boolean isBehindProxy() {
        try {
            var spigot = Bukkit.spigot();

            if (spigot.getConfig().getBoolean("settings.bungeecord")) {
                return true;
            }

            try {
                var paperConfigMethod = spigot.getClass().getMethod("getPaperConfig");
                var paperConfig = paperConfigMethod.invoke(spigot);

                if (paperConfig instanceof org.bukkit.configuration.Configuration configuration) {
                    return configuration.getBoolean("settings.velocity-support.enabled")
                           || configuration.getBoolean("proxies.velocity.enabled");
                }
            } catch (NoSuchMethodException ignored) {
                // Not Paper, so there is no velocity-support configuration.
            }
        } catch (ReflectiveOperationException ignored) {
            // The configuration could not be read, assume there is no proxy.
        }

        return false;
    }

    /**
     * Creates a Paper {@code PlayerProfile} with an exact UUID, or {@code null} when running on Spigot.
     */
    public static Object createProfileExact(UUID uuid, String name) {
        if (!PAPER) {
            return null;
        }

        try {
            return Bukkit.class.getMethod("createProfileExact", UUID.class, String.class).invoke(null, uuid, name);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    /**
     * Applies a Paper profile to a pre-login event. No-op on Spigot, which has no such API.
     */
    public static boolean setPlayerProfile(AsyncPlayerPreLoginEvent event, Object profile) {
        if (profile == null) {
            return false;
        }

        try {
            var profileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            event.getClass().getMethod("setPlayerProfile", profileClass).invoke(event, profile);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static void runSyncAndWait(Runnable runnable, PaperLibreLogin plugin) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            var future = new CompletableFuture<Void>();
            Bukkit.getScheduler().runTask(plugin.getBootstrap(), () -> {
                runnable.run();
                future.complete(null);
            });
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
