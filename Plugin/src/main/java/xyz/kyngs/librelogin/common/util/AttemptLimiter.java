/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts attempts per key and refuses further attempts after a limit is reached inside a time window.
 * <p>
 * Unlike {@link RateLimiter}, which only allows a single attempt per window, this is intended for
 * secret guessing protection (for example 2FA codes), where a small number of legitimate retries
 * should still be possible.
 *
 * @param <T> key type
 */
public class AttemptLimiter<T> {

    private final Cache<T, AtomicInteger> attempts;
    private final int maxAttempts;

    public AttemptLimiter(int maxAttempts, long duration, TimeUnit unit) {
        this.maxAttempts = maxAttempts;
        this.attempts = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(duration, unit)
                .build();
    }

    /**
     * @param key the key to count an attempt for
     * @return true if the attempt is allowed, false if the limit has been exhausted
     */
    public boolean tryAcquire(T key) {
        var counter = attempts.get(key, ignored -> new AtomicInteger());

        return counter.incrementAndGet() <= maxAttempts;
    }

    /**
     * Clears the counter for the key, typically after a successful attempt.
     */
    public void reset(T key) {
        attempts.invalidate(key);
    }

}
