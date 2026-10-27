/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common;

import xyz.kyngs.librelogin.api.Logger;

import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * A logger implementation backed by {@link java.util.logging}, which is available on every Bukkit derived platform.
 */
public class JULogger implements Logger {

    private final java.util.logging.Logger logger;
    private final Supplier<Boolean> debug;

    public JULogger(java.util.logging.Logger logger, Supplier<Boolean> debug) {
        this.logger = logger;
        this.debug = debug;
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void info(String message, Throwable throwable) {
        logger.log(Level.INFO, message, throwable);
    }

    @Override
    public void warn(String message) {
        logger.warning(message);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        logger.log(Level.WARNING, message, throwable);
    }

    @Override
    public void error(String message) {
        logger.severe(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    @Override
    public void debug(String message) {
        if (debug.get()) {
            logger.info("[DEBUG] " + message);
        }
    }

    @Override
    public void debug(String message, Throwable throwable) {
        if (debug.get()) {
            logger.log(Level.INFO, "[DEBUG] " + message, throwable);
        }
    }
}
