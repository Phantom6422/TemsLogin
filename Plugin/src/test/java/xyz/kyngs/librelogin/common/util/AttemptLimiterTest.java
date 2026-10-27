/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttemptLimiterTest {

    @Test
    void allowsAttemptsUpToTheLimit() {
        var limiter = new AttemptLimiter<String>(3, 1, TimeUnit.MINUTES);

        assertTrue(limiter.tryAcquire("player"));
        assertTrue(limiter.tryAcquire("player"));
        assertTrue(limiter.tryAcquire("player"));
        assertFalse(limiter.tryAcquire("player"));
    }

    @Test
    void tracksEachKeySeparately() {
        var limiter = new AttemptLimiter<String>(1, 1, TimeUnit.MINUTES);

        assertTrue(limiter.tryAcquire("first"));
        assertFalse(limiter.tryAcquire("first"));
        assertTrue(limiter.tryAcquire("second"));
    }

    @Test
    void resetClearsTheCounter() {
        var limiter = new AttemptLimiter<String>(1, 1, TimeUnit.MINUTES);

        assertTrue(limiter.tryAcquire("player"));
        assertFalse(limiter.tryAcquire("player"));

        limiter.reset("player");

        assertTrue(limiter.tryAcquire("player"));
    }
}
