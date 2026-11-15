/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity;

import java.util.regex.Pattern;

/**
 * Checks whether the running proxy satisfies the minimum Velocity version required by TemsLogin.
 * <p>
 * The API is built against Velocity 4.3.x, so older proxies are rejected before any plugin state is
 * initialised instead of failing later with a {@link NoSuchMethodError} or a partial enable.
 */
final class VelocityRequirements {

    /**
     * The oldest Velocity version this fork supports.
     */
    static final String MINIMUM_VERSION = "4.3.0";

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
    private static final int MINIMUM_MAJOR = 4;
    private static final int MINIMUM_MINOR = 3;
    private static final int MINIMUM_PATCH = 0;

    private VelocityRequirements() {
    }

    /**
     * Returns whether the given proxy version string is at least {@link #MINIMUM_VERSION}.
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

        var major = Integer.parseInt(matcher.group(1));
        var minor = Integer.parseInt(matcher.group(2));
        var patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));

        if (major != MINIMUM_MAJOR) {
            return major > MINIMUM_MAJOR;
        }
        if (minor != MINIMUM_MINOR) {
            return minor > MINIMUM_MINOR;
        }
        return patch >= MINIMUM_PATCH;
    }
}
