/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.velocity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityRequirementsTest {

    @Test
    void acceptsMinimumAndNewerVersions() {
        assertTrue(VelocityRequirements.isSupported("4.3.0"));
        assertTrue(VelocityRequirements.isSupported("4.3.0-SNAPSHOT"));
        assertTrue(VelocityRequirements.isSupported("4.3.2"));
        assertTrue(VelocityRequirements.isSupported("4.3.2-SNAPSHOT"));
        assertTrue(VelocityRequirements.isSupported("4.3.2-SNAPSHOT-48"));
        assertTrue(VelocityRequirements.isSupported("4.4"));
        assertTrue(VelocityRequirements.isSupported("5.0.0"));
    }

    @Test
    void rejectsOlderVersions() {
        assertFalse(VelocityRequirements.isSupported("4.2.1"));
        assertFalse(VelocityRequirements.isSupported("4.2.1-SNAPSHOT"));
        assertFalse(VelocityRequirements.isSupported("3.4.0"));
        assertFalse(VelocityRequirements.isSupported("3.x"));
    }

    @Test
    void ignoresTextAroundTheVersion() {
        assertTrue(VelocityRequirements.isSupported("Velocity 4.3.0"));
        assertFalse(VelocityRequirements.isSupported("Velocity 4.2.9"));
    }

    @Test
    void rejectsMissingOrUnparseableVersions() {
        assertFalse(VelocityRequirements.isSupported(null));
        assertFalse(VelocityRequirements.isSupported(""));
        assertFalse(VelocityRequirements.isSupported("SNAPSHOT"));
    }
}
