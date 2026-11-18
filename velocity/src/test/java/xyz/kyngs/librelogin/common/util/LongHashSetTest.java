/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongHashSetTest {

    @Test
    void findsStoredValuesAfterFreeze() {
        var set = new LongHashSet(4);
        set.add(1L);
        set.add(-2L);
        set.add(3_000_000_000L);
        set.freeze();

        assertTrue(set.contains(1L));
        assertTrue(set.contains(-2L));
        assertTrue(set.contains(3_000_000_000L));
    }

    @Test
    void doesNotFindMissingValues() {
        var set = new LongHashSet(4);
        set.add(10L);
        set.add(20L);
        set.freeze();

        assertFalse(set.contains(0L));
        assertFalse(set.contains(15L));
        assertFalse(set.contains(21L));
    }

    @Test
    void deduplicatesOnFreeze() {
        var set = new LongHashSet(4);
        set.add(5L);
        set.add(5L);
        set.add(6L);
        set.freeze();

        assertEquals(2, set.size());
    }

    @Test
    void handlesEmptySet() {
        var set = new LongHashSet(1);
        set.freeze();

        assertEquals(0, set.size());
        assertFalse(set.contains(1L));
    }

    @Test
    void growsBeyondInitialCapacity() {
        var set = new LongHashSet(1);

        for (var i = 0; i < 1000; i++) {
            set.add(i);
        }

        set.freeze();

        assertEquals(1000, set.size());
        assertTrue(set.contains(999L));
        assertFalse(set.contains(1000L));
    }

    @Test
    void rejectsWritesAfterFreeze() {
        var set = new LongHashSet(1);
        set.freeze();

        assertThrows(IllegalStateException.class, () -> set.add(1L));
    }
}
