/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.util;

import java.util.Arrays;

/**
 * A compact, build-once set of 64-bit values backed by a sorted primitive array.
 * <p>
 * The forbidden password list contains close to a million entries. Keeping them as {@link String}s
 * in a {@link java.util.HashSet} costs well over 100 MB, while the same data as sorted 64-bit
 * hashes needs about 8 MB and still answers lookups in O(log n).
 * <p>
 * Values must be added before {@link #freeze()} is called; afterwards the set is immutable and safe
 * to read from multiple threads.
 */
public final class LongHashSet {

    private long[] values;
    private int size;
    private boolean frozen;

    public LongHashSet(int expectedSize) {
        this.values = new long[Math.max(16, expectedSize)];
    }

    public void add(long value) {
        if (frozen) {
            throw new IllegalStateException("Cannot add to a frozen LongHashSet");
        }

        if (size == values.length) {
            values = Arrays.copyOf(values, values.length + (values.length >> 1) + 1);
        }

        values[size++] = value;
    }

    /**
     * Sorts and de-duplicates the values, releasing the unused tail of the backing array.
     */
    public void freeze() {
        if (frozen) {
            return;
        }

        Arrays.sort(values, 0, size);

        var unique = 0;

        for (var i = 0; i < size; i++) {
            if (unique == 0 || values[unique - 1] != values[i]) {
                values[unique++] = values[i];
            }
        }

        size = unique;
        values = Arrays.copyOf(values, size);
        frozen = true;
    }

    public boolean contains(long value) {
        var low = 0;
        var high = size - 1;

        while (low <= high) {
            var mid = (low + high) >>> 1;
            var midValue = values[mid];

            if (midValue < value) {
                low = mid + 1;
            } else if (midValue > value) {
                high = mid - 1;
            } else {
                return true;
            }
        }

        return false;
    }

    public int size() {
        return size;
    }
}
