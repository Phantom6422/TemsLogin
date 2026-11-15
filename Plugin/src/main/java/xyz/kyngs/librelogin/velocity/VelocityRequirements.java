/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity;

import java.util.regex.Pattern;

/**
 * Checks whether the running proxy is a supported Velocity 4.x release.
 * <p>
 * TemsLogin is built against the Velocity 4.0.0 API and supports the whole 4.x line. Older 3.x
 * proxies and untested future majors are rejected before any plugin state is initialised instead
 * of failing later with a {@link NoSuchMethodError} or a partial enable.
 */
final class VelocityRequirements {

    /**
     * Human-readable description of the supported proxy line.
     */
    static final String SUPPORTED_VERSION = "4.x (4.0.0 or newer)";

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final int SUPPORTED_MAJOR = 4;

    private VelocityRequirements() {
    }

    /**
     * Returns whether the given proxy version string is a supported Velocity 4.x release.
     * <p>
     * Velocity reports versions such as {@code 4.3.2-SNAPSHOT} or {@code 4.3.2-SNAPSHOT-48}. Only the
     * numeric {@code major.minor.patch} part is compared, so pre-release and build suffixes never make
     * a supported release look older than it is. Unparseable versions are treated as unsupported.
     *
     * @param rawVersion the version reported by {@code ProxyServer#getVersion()}
     * @return {@code true} when the proxy is supported
     */
    static boolean isSupported(String rawVersion) {
        if (rawVersion == null) {
            return false;
        }

        var matcher = VERSION_PATTERN.matcher(rawVersion);

        if (!matcher.find()) {
            return false;
        }

        return Integer.parseInt(matcher.group(1)) == SUPPORTED_MAJOR;
    }
}
