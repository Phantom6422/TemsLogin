/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.paper;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.CommandManager;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import xyz.kyngs.librelogin.api.Logger;
import xyz.kyngs.librelogin.api.database.User;
import xyz.kyngs.librelogin.api.event.exception.EventCancelledException;
import xyz.kyngs.librelogin.common.AuthenticLibreLogin;
import xyz.kyngs.librelogin.common.JULogger;
import xyz.kyngs.librelogin.common.image.AuthenticImageProjector;
import xyz.kyngs.librelogin.common.util.CancellableTask;
import xyz.kyngs.librelogin.paper.protocol.PacketListener;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;

import static xyz.kyngs.librelogin.common.config.ConfigurationKeys.DEBUG;

public class PaperLibreLogin extends AuthenticLibreLogin<Player, World> implements PlatformLifecycle {

    private final PaperBootstrap bootstrap;
    private PaperListeners listeners;
    private boolean started;

    public PaperLibreLogin(PaperBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        this.started = false;

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(bootstrap));

        PacketEvents.getAPI().getSettings()
                .checkForUpdates(false)
                .bStats(false)
                .debug(true);

        PacketEvents.getAPI().load();
    }

    public PaperBootstrap getBootstrap() {
        return bootstrap;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return bootstrap.getResource(name);
    }

    @Override
    public File getDataFolder() {
        return bootstrap.getDataFolder();
    }

    @Override
    public String getVersion() {
        return bootstrap.getDescription().getVersion();
    }

    @Override
    public boolean isPresent(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    @Override
    public boolean multiProxyEnabled() {
        return false;
    }

    @Override
    public Player getPlayerForUUID(UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }

    @Override
    protected PaperPlatformHandle providePlatformHandle() {
        return new PaperPlatformHandle(this);
    }

    @Override
    protected Logger provideLogger() {
        return new JULogger(bootstrap.getLogger(), () -> getConfiguration().get(DEBUG));
    }

    @Override
    public CommandManager<?, ?, ?, ?, ?, ?> provideManager() {
        return new BukkitCommandManager(bootstrap);
    }

    @Override
    protected boolean mainThread() {
        return Bukkit.isPrimaryThread() && started;
    }

    @Override
    public Player getPlayerFromIssuer(CommandIssuer issuer) {
        var bukkitIssuer = (BukkitCommandIssuer) issuer;

        return bukkitIssuer.getPlayer();
    }

    @Override
    public void enablePlatform() {
        enable();
    }

    @Override
    public void disablePlatform() {
        disable();
    }

    @Override
    protected void disable() {
        PacketEvents.getAPI().terminate();

        // Always run the common cleanup: super.disable() is null-safe and also cancels scheduled tasks
        // and detaches the log filter, which a partially initialised plugin would otherwise leak.
        super.disable();
    }

    @Override
    protected void enable() {

        logger = provideLogger();

        if (Bukkit.getOnlineMode()) {
            getLogger().error("!!!The server is running in online mode! LibreLogin won't start unless you set it to false!!!");
            disable();
            return;
        }

        if (PaperUtil.isBehindProxy()) {
            getLogger().error("!!!This server is running under a proxy, LibreLogin won't start!!!");
            getLogger().error("If you want to use LibreLogin under a proxy, place it on the proxy and remove it from the server.");
            disable();
            return;
        }

        try {
            super.enable();
        } catch (ShutdownException e) {
            return;
        }

        Permissions.register();

        var provider = getEventProvider();

        provider.subscribe(provider.getTypes().authenticated, event -> {
            var player = event.getPlayer();
            if (player == null) return;
            // Unlike Paper, Spigot has no Player#setInvisible, so this must go through the compatibility helper.
            PaperUtil.setInvisible(player, false);
        });

        listeners = new PaperListeners(this);

        Bukkit.getPluginManager().registerEvents(listeners, bootstrap);
        Bukkit.getPluginManager().registerEvents(new Blockers(this), bootstrap);

        if (PaperUtil.isPaper()) {
            Bukkit.getPluginManager().registerEvents(new PaperChatBlocker(this), bootstrap);
        }

        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener(listeners));

        started = true;
    }

    @Override
    public void authorize(Player player, User user, Audience audience) {
        try {

            var location = listeners.getSpawnLocationCache().getIfPresent(player);

            if (location == null) {
                var world = getServerHandler().chooseLobbyServer(user, player, true, false);

                if (world == null) {
                    PaperUtil.kick(player, getMessages().getMessage("kick-no-lobby"));
                    return;
                }

                location = world.getSpawnLocation();
            } else {
                listeners.getSpawnLocationCache().invalidate(player);
            }

            PaperUtil.teleport(this, player, location);

        } catch (EventCancelledException ignored) {
        }
    }

    @Override
    public CancellableTask delay(Runnable runnable, long delayInMillis) {
        var task = Bukkit.getScheduler().runTaskLaterAsynchronously(bootstrap, runnable, delayInMillis / 50);
        return task::cancel;
    }

    @Override
    public CancellableTask repeat(Runnable runnable, long delayInMillis, long repeatInMillis) {
        var task = Bukkit.getScheduler()
                .runTaskTimerAsynchronously(bootstrap, runnable, delayInMillis / 50, repeatInMillis / 50);
        return task::cancel;
    }

    @Override
    public boolean pluginPresent(String pluginName) {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }

    @Override
    protected AuthenticImageProjector<Player, World> provideImageProjector() {
        return null;
    }


    @Override
    protected void shutdownProxy(int code) {
        bootstrap.disable();
        bootstrap.getServer().shutdown();
        throw new ShutdownException();
    }

    @Override
    public Audience getAudienceFromIssuer(CommandIssuer issuer) {
        return PaperUtil.getAudience(((BukkitCommandIssuer) issuer).getIssuer());
    }
}
