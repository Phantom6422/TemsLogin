/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.kyngs.librelogin.api.provider.LibreLoginProvider;
import xyz.kyngs.librelogin.api.util.SemanticVersion;

public class PaperBootstrap extends JavaPlugin implements LibreLoginProvider<Player, World> {

    private PlatformLifecycle libreLogin;
    private boolean failed;

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return id == null ?
                null
                : id.equals("void") ? new VoidWorldGenerator() : null;
    }

    @Override
    public void onLoad() {
        if (!isPaperPlatform()) {
            getLogger().info("Loading libraries...");

            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                getLogger().severe("Failed to create the plugin data folder");
            }

            try {
                new SpigotLibraryManager(this).configureFromJSON();
            } catch (Exception e) {
                getLogger().severe("Failed to load libraries, stopping server to prevent damage");
                e.printStackTrace();
                failed = true;
                getServer().shutdown();
                return;
            }
        }

        try {
            // The platform class references third-party libraries, so it must only be loaded after
            // the Spigot library manager has injected them into the plugin class loader.
            var platformClass = Class.forName("xyz.kyngs.librelogin.paper.PaperLibreLogin");
            libreLogin = (PlatformLifecycle) platformClass.getConstructor(PaperBootstrap.class).newInstance(this);
        } catch (Throwable throwable) {
            getLogger().severe("Failed to initialize TemsLogin, is PacketEvents installed and up to date?");
            throwable.printStackTrace();
            failed = true;
        }
    }

    @Override
    public void onEnable() {
        if (failed || libreLogin == null) {
            getLogger().severe("TemsLogin failed to initialize, disabling the plugin. Check the console for details.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Bootstrapping TemsLogin...");
        libreLogin.enablePlatform();
    }

    @Override
    public void onDisable() {
        if (libreLogin != null) {
            libreLogin.disablePlatform();
        }
    }

    @Override
    public PlatformLifecycle getLibreLogin() {
        return libreLogin;
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public SemanticVersion getParsedVersion() {
        return SemanticVersion.parse(getVersion());
    }

    protected void disable() {
        setEnabled(false);
    }

    private static boolean isPaperPlatform() {
        try {
            Class.forName("io.papermc.paper.plugin.loader.PluginLoader");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

}
