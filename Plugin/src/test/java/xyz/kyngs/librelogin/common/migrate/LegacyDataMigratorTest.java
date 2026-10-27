/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.migrate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.kyngs.librelogin.api.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDataMigratorTest {

    private static final Logger LOGGER = new Logger() {
        @Override
        public void info(String message) {
        }

        @Override
        public void info(String message, Throwable throwable) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void warn(String message, Throwable throwable) {
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void error(String message, Throwable throwable) {
        }

        @Override
        public void debug(String message) {
        }

        @Override
        public void debug(String message, Throwable throwable) {
        }
    };

    @Test
    void movesTheWholeFolderWhenTheTargetDoesNotExist(@TempDir Path parent) throws IOException {
        var legacy = new File(parent.toFile(), "librelogin");
        assertTrue(legacy.mkdirs());
        write(new File(legacy, "config.conf"), "legacy-config");

        var target = new File(parent.toFile(), "temslogin");

        assertTrue(LegacyDataMigrator.migrate(parent.toFile(), target, LOGGER));
        assertFalse(legacy.exists());
        assertEquals("legacy-config", read(new File(target, "config.conf")));
    }

    @Test
    void copiesMissingFilesWhenTheTargetAlreadyExists(@TempDir Path parent) throws IOException {
        var legacy = new File(parent.toFile(), "librelogin");
        assertTrue(legacy.mkdirs());
        write(new File(legacy, "config.conf"), "legacy-config");
        write(new File(legacy, "messages.conf"), "legacy-messages");
        write(new File(legacy, "user-data.db"), "legacy-database");

        var target = new File(parent.toFile(), "temslogin");
        assertTrue(target.mkdirs());
        write(new File(target, "config.conf"), "current-config");

        assertTrue(LegacyDataMigrator.migrate(parent.toFile(), target, LOGGER));

        // The already configured file wins.
        assertEquals("current-config", read(new File(target, "config.conf")));
        // Missing files are imported.
        assertEquals("legacy-messages", read(new File(target, "messages.conf")));
        assertEquals("legacy-database", read(new File(target, "user-data.db")));
    }

    @Test
    void prefersLibreloginOverOlderFolders(@TempDir Path parent) throws IOException {
        var newest = new File(parent.toFile(), "librelogin");
        var oldest = new File(parent.toFile(), "LibrePremium");
        assertTrue(newest.mkdirs());
        assertTrue(oldest.mkdirs());
        write(new File(newest, "config.conf"), "newest");
        write(new File(oldest, "config.conf"), "oldest");

        var target = new File(parent.toFile(), "temslogin");

        assertTrue(LegacyDataMigrator.migrate(parent.toFile(), target, LOGGER));
        assertEquals("newest", read(new File(target, "config.conf")));
        assertTrue(oldest.exists(), "Only the first matching legacy folder should be imported");
    }

    @Test
    void doesNothingWhenThereIsNoLegacyFolder(@TempDir Path parent) {
        var target = new File(parent.toFile(), "temslogin");

        assertFalse(LegacyDataMigrator.migrate(parent.toFile(), target, LOGGER));
        assertFalse(target.exists());
    }

    @Test
    void copiesSqliteSidecarFiles(@TempDir Path parent) throws IOException {
        var legacy = new File(parent.toFile(), "librelogin");
        assertTrue(legacy.mkdirs());
        write(new File(legacy, "user-data.db"), "main");
        write(new File(legacy, "user-data.db-wal"), "wal");

        var target = new File(parent.toFile(), "temslogin");
        assertTrue(target.mkdirs());

        assertTrue(LegacyDataMigrator.migrate(parent.toFile(), target, LOGGER));
        assertEquals("main", read(new File(target, "user-data.db")));
        assertEquals("wal", read(new File(target, "user-data.db-wal")));
    }

    private static void write(File file, String content) throws IOException {
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
    }

    private static String read(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }
}
