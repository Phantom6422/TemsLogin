/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity.integration;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import ua.nanit.limbo.NanoLimbo;
import ua.nanit.limbo.server.LimboServer;
import ua.nanit.limbo.server.data.InfoForwarding;
import xyz.kyngs.librelogin.common.integration.nanolimbo.NanoLimboIntegration;

public class VelocityNanoLimboIntegration extends NanoLimboIntegration<RegisteredServer> {

    private final ClassLoader classLoader;
    private final ProxyServer proxyServer;

    public VelocityNanoLimboIntegration(ProxyServer proxyServer, String portRange) {
        super(portRange);
        this.classLoader = NanoLimbo.class.getClassLoader();
        this.proxyServer = proxyServer;
    }

    @Override
    public RegisteredServer createLimbo(String serverName) {
        InetSocketAddress address = findLocalAvailableAddress().orElseThrow(() -> new IllegalStateException("Cannot find available port for limbo server!"));
        LimboServer server = createLimboServer(address);
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                server.stop();
            } catch (Exception ignored) {
                // The server never started, stopping it is only best-effort cleanup.
            }
            // Do not register a dead endpoint: routing players to a port with nothing listening kicks them.
            return null;
        }
        return proxyServer.registerServer(new ServerInfo(serverName, address));
    }

    @Override
    protected InfoForwarding createForwarding() {
        // Velocity does not expose the forwarding mode through its public API, and the internal proxy artifact
        // is not published for every version. Reading it reflectively keeps this working across proxy versions
        // without a compile-time dependency on com.velocitypowered.proxy.
        var configuration = proxyServer.getConfiguration();

        try {
            var mode = configuration.getClass().getMethod("getPlayerInfoForwardingMode").invoke(configuration);
            var modeName = mode instanceof Enum<?> value ? value.name() : String.valueOf(mode);

            return switch (modeName) {
                case "NONE" -> FORWARDING_FACTORY.none();
                case "LEGACY" -> FORWARDING_FACTORY.legacy();
                case "MODERN" -> FORWARDING_FACTORY.modern(forwardingSecret(configuration));
                case "BUNGEEGUARD" ->
                        FORWARDING_FACTORY.bungeeGuard(Collections.singleton(new String(forwardingSecret(configuration), StandardCharsets.UTF_8)));
                default -> throw new IllegalStateException("Unknown Velocity forwarding mode: " + modeName);
            };
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read the Velocity forwarding configuration", e);
        }
    }

    private static byte[] forwardingSecret(Object configuration) throws ReflectiveOperationException {
        return (byte[]) configuration.getClass().getMethod("getForwardingSecret").invoke(configuration);
    }

    @Override
    protected ClassLoader classLoader() {
        return classLoader;
    }

}
