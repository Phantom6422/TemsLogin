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
    void acceptsTheWholeVelocity4Line() {
        assertTrue(VelocityRequirements.isSupported("4.0.0"));
        assertTrue(VelocityRequirements.isSupported("4.0.0-SNAPSHOT"));
        assertTrue(VelocityRequirements.isSupported("4.1.0"));
        assertTrue(VelocityRequirements.isSupported("4.2.1"));
        assertTrue(VelocityRequirements.isSupported("4.3.0"));
        assertTrue(VelocityRequirements.isSupported("4.3.2-SNAPSHOT"));
        assertTrue(VelocityRequirements.isSupported("4.3.2-SNAPSHOT-48"));
        assertTrue(VelocityRequirements.isSupported("4.9.9"));
    }

    @Test
    void rejectsOtherMajorVersions() {
        assertFalse(VelocityRequirements.isSupported("3.4.0"));
        assertFalse(VelocityRequirements.isSupported("3.9.9"));
        assertFalse(VelocityRequirements.isSupported("3.x"));
        assertFalse(VelocityRequirements.isSupported("5.0.0"));
        assertFalse(VelocityRequirements.isSupported("5.1.0"));
    }

    @Test
    void ignoresTextAroundTheVersion() {
        assertTrue(VelocityRequirements.isSupported("Velocity 4.3.0"));
        assertFalse(VelocityRequirements.isSupported("Velocity 3.4.0"));
    }

    @Test
    void rejectsMissingOrUnparseableVersions() {
        assertFalse(VelocityRequirements.isSupported(null));
        assertFalse(VelocityRequirements.isSupported(""));
        assertFalse(VelocityRequirements.isSupported("SNAPSHOT"));
    }
}
