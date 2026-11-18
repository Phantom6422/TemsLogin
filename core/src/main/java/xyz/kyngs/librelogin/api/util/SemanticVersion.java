/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.api.util;

/**
 * A basic record to hold semantic version information.
 *
 * @param major The major version.
 * @param minor The minor version.
 * @param patch The patch version.
 * @param dev   Whether this is a development version.
 * @author kyngs
 */
public record SemanticVersion(int major, int minor, int patch, boolean dev) implements Comparable<SemanticVersion> {

    /**
     * Parses a semantic version from a string with format major.minor.patch(-SNAPSHOT).
     * <p>
     * An optional leading {@code v} is allowed (e.g. {@code v1.2.3}), missing minor or patch
     * components default to zero, and any pre-release or build metadata suffix is ignored
     * (e.g. {@code 1.2.3-rc1}, {@code 1.2.3+build.5}).
     *
     * @param version The string to parse.
     * @return The parsed semantic version.
     */
    public static SemanticVersion parse(String version) {
        if (version == null) {
            throw new IllegalArgumentException("Version must not be null");
        }

        String normalized = version.trim();

        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }

        boolean dev = normalized.endsWith("-SNAPSHOT");

        if (dev) {
            normalized = normalized.substring(0, normalized.length() - "-SNAPSHOT".length());
        }

        // Strip pre-release and build metadata suffixes, they do not affect ordering here.
        int suffix = normalized.length();
        int dash = normalized.indexOf('-');
        if (dash >= 0) suffix = Math.min(suffix, dash);
        int plus = normalized.indexOf('+');
        if (plus >= 0) suffix = Math.min(suffix, plus);
        normalized = normalized.substring(0, suffix);

        String[] split = normalized.split("\\.");

        return new SemanticVersion(parseComponent(split, 0), parseComponent(split, 1), parseComponent(split, 2), dev);
    }

    private static int parseComponent(String[] split, int index) {
        if (index >= split.length) {
            return 0;
        }

        String component = split[index].trim();
        int end = 0;

        while (end < component.length() && Character.isDigit(component.charAt(end))) {
            end++;
        }

        if (end == 0) {
            return 0;
        }

        try {
            return Integer.parseInt(component.substring(0, end));
        } catch (NumberFormatException ignored) {
            // The component does not fit into an int, so it is effectively larger than any valid one.
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Compares this version with the specified version.
     *
     * @param other the version to compare to
     * @return -1 if this version is less than the specified version, 0 if they are equal, and 1 if this version is greater
     */
    @Override
    public int compareTo(SemanticVersion other) {
        return compare(other);
    }

    /**
     * Compares this version with the specified version.
     *
     * @param other the version to compare to
     * @return -1 if this version is less than the specified version, 0 if they are equal, and 1 if this version is greater
     */
    public int compare(SemanticVersion other) {
        if (major != other.major) {
            return major < other.major ? -1 : 1;
        }
        if (minor != other.minor) {
            return minor < other.minor ? -1 : 1;
        }
        if (patch != other.patch) {
            return patch < other.patch ? -1 : 1;
        }
        return dev ?
                (other.dev ? 0 : -1)
                :
                (other.dev ? 1 : 0);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + (dev ? "-SNAPSHOT" : "");
    }
}
