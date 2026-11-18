/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import xyz.kyngs.librarian.VelocityLibraryManager;
import xyz.kyngs.librelogin.BuildParameters;
import xyz.kyngs.librelogin.api.LibreLoginPlugin;
import xyz.kyngs.librelogin.api.provider.LibreLoginProvider;
import xyz.kyngs.librelogin.api.util.SemanticVersion;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Plugin(
        id = "temslogin",
        name = "TemsLogin",
        version = BuildParameters.VERSION,
        authors = {"kyngs", "Tems"},
        url = "https://phantomsnetwork.org",
        dependencies = {
                @Dependency(id = "floodgate", optional = true),
                @Dependency(id = "luckperms", optional = true),
                @Dependency(id = "protocolize", optional = true),
                @Dependency(id = "redisbungee", optional = true),
                @Dependency(id = "nanolimbovelocity", optional = true)
        }
)
public class VelocityBootstrap implements LibreLoginProvider<Player, RegisteredServer> {

    @Inject
    PluginDescription pluginDescription;

    ProxyServer server;
    private final Logger logger;
    private final VelocityLibreLogin libreLogin;

    @Inject
    public VelocityBootstrap(ProxyServer server, Injector injector, Logger logger, PluginContainer container) {
        this.server = server;
        this.logger = logger;

        // Refuse to start on unsupported proxies instead of enabling with an incompatible API.
        var proxyVersion = server.getVersion().getVersion();

        if (!VelocityRequirements.isSupported(proxyVersion)) {
            var message = "TemsLogin requires Velocity " + VelocityRequirements.SUPPORTED_VERSION
                    + ", but this proxy reports " + proxyVersion + ". TemsLogin will not be enabled.";
            failClosed(message, null);
            throw new IllegalStateException(message);
        }

        logger.info("Loading libraries...");

        // This is a very ugly hack to be able to load libraries in the constructor
        // We cannot pass this as a parameter to the constructor because the plugin is technically still not loaded
        // And, we cannot past the container as a parameter to the constructor because the proxy still did not assign the instance to it.
        // So, we have to "mock" the container and pass this as the instance. I'm kinda surprised this works, but in theory could break in the future.
        // Allocated once and shut down after loading. Creating a new executor on every getExecutorService()
        // call would leak one non-daemon thread per call.
        var libraryExecutor = Executors.newSingleThreadExecutor(runnable -> {
            var thread = new Thread(runnable, "TemsLogin-Library-Loader");
            thread.setDaemon(true);
            return thread;
        });

        try {
            var libraryManager = new VelocityLibraryManager<PluginContainer>(server.getPluginManager(), new PluginContainer() {
                @Override
                public PluginDescription getDescription() {
                    return container.getDescription();
                }

                @Override
                public Optional<?> getInstance() {
                    return Optional.of(this);
                }

                @Override
                public ExecutorService getExecutorService() {
                    return libraryExecutor;
                }
            });

            libraryManager.configureFromJSON();
        } catch (Throwable t) {
            failClosed("TemsLogin failed to load its libraries, shutting down the proxy to avoid running without authentication.", t);
            throw t instanceof RuntimeException runtime ? runtime : new IllegalStateException("Failed to load TemsLogin libraries", t);
        } finally {
            libraryExecutor.shutdown();
        }

        libreLogin = new VelocityLibreLogin(this);
        injector.injectMembers(libreLogin);
    }

    @Subscribe
    public void onInitialization(ProxyInitializeEvent event) {
        try {
            libreLogin.enable();

            server.getEventManager().register(this, new Blockers(libreLogin.getAuthorizationProvider(), libreLogin.getConfiguration(), libreLogin.getMessages()));
            server.getEventManager().register(this, new VelocityListeners(libreLogin));
        } catch (Throwable t) {
            // Never leave an authentication proxy running without its listeners: the backends are in
            // offline mode and would accept any username.
            try {
                libreLogin.disable();
            } catch (Throwable ignored) {
                // The plugin is already in a broken state, the shutdown below is what matters.
            }
            failClosed("TemsLogin failed to initialize, shutting down the proxy to avoid running without authentication.", t);
        }
    }

    private void failClosed(String message, Throwable throwable) {
        if (throwable == null) {
            logger.error(message);
        } else {
            logger.error(message, throwable);
        }

        try {
            server.shutdown(Component.text(message));
        } catch (Throwable shutdownFailure) {
            logger.error("Could not shut down the proxy after a TemsLogin initialization failure", shutdownFailure);
        }
    }

    @Override
    public LibreLoginPlugin<Player, RegisteredServer> getLibreLogin() {
        return libreLogin;
    }

    @Override
    public String getVersion() {
        return pluginDescription.getVersion().orElse(null);
    }

    @Override
    public SemanticVersion getParsedVersion() {
        var version = getVersion();
        return version == null ? null : SemanticVersion.parse(version);
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        libreLogin.disable();
    }
}
