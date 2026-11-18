/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticVersionTest {

    @Test
    void parsesPlainVersion() {
        assertEquals(new SemanticVersion(1, 2, 3, false), SemanticVersion.parse("1.2.3"));
    }

    @Test
    void parsesVersionWithVPrefix() {
        assertEquals(new SemanticVersion(0, 25, 1, false), SemanticVersion.parse("v0.25.1"));
    }

    @Test
    void parsesSnapshotVersion() {
        assertEquals(new SemanticVersion(0, 26, 1, true), SemanticVersion.parse("0.26.1-SNAPSHOT"));
    }

    @Test
    void parsesSnapshotVersionWithVPrefix() {
        assertEquals(new SemanticVersion(0, 26, 1, true), SemanticVersion.parse("v0.26.1-SNAPSHOT"));
    }

    @Test
    void defaultsMissingComponentsToZero() {
        assertEquals(new SemanticVersion(0, 0, 0, false), SemanticVersion.parse("v0"));
        assertEquals(new SemanticVersion(1, 0, 0, false), SemanticVersion.parse("v1"));
        assertEquals(new SemanticVersion(1, 2, 0, false), SemanticVersion.parse("1.2"));
    }

    @Test
    void ignoresPreReleaseAndBuildMetadata() {
        assertEquals(new SemanticVersion(1, 2, 3, false), SemanticVersion.parse("v1.2.3-rc1"));
        assertEquals(new SemanticVersion(1, 2, 3, false), SemanticVersion.parse("1.2.3+build.5"));
    }

    @Test
    void comparesSnapshotAgainstRelease() {
        var snapshot = SemanticVersion.parse("0.26.1-SNAPSHOT");
        var release = SemanticVersion.parse("v0.26.1");

        assertTrue(snapshot.compare(release) < 0);
        assertTrue(release.compare(snapshot) > 0);
        assertEquals(0, release.compare(SemanticVersion.parse("0.26.1")));
    }
}
