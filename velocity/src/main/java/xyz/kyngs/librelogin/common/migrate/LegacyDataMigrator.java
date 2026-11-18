/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.migrate;

import xyz.kyngs.librelogin.api.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Imports the data folder of an older plugin name (or of upstream LibreLogin) into the TemsLogin folder.
 * <p>
 * The whole legacy folder is moved when the target does not exist yet. If it does exist, only files that are
 * missing in the target are copied, so an already configured installation is never overwritten. Configuration
 * revisions are upgraded later by the regular config/messages migrators.
 */
public final class LegacyDataMigrator {

    /**
     * Folder names used by LibreLogin and by the LibrePremium predecessors.
     */
    public static final List<String> LEGACY_FOLDERS = List.of("librelogin", "librepremium", "LibreLogin", "LibrePremium");

    /**
     * User files that must survive the rebrand. SQLite databases are handled separately.
     */
    public static final List<String> LEGACY_FILES = List.of(
            "config.conf",
            "messages.conf",
            "forbidden-passwords.txt"
    );

    private LegacyDataMigrator() {
    }

    /**
     * @param parent the directory containing the plugin data folders (typically {@code plugins/})
     * @param target the current data folder
     * @param logger logger to report progress to
     * @return true if a legacy folder was found and handled
     */
    public static boolean migrate(File parent, File target, Logger logger) {
        if (parent == null || target == null) {
            return false;
        }

        for (String legacyName : LEGACY_FOLDERS) {
            var legacy = new File(parent, legacyName);

            if (legacy.getAbsoluteFile().equals(target.getAbsoluteFile()) || !legacy.isDirectory()) {
                continue;
            }

            if (!target.exists()) {
                logger.info("Migrating all data from the legacy folder '%s'...".formatted(legacyName));

                if (legacy.renameTo(target)) {
                    logger.info("Successfully moved '%s' to '%s'".formatted(legacyName, target.getName()));
                    return true;
                }

                logger.warn("Could not move '%s', copying the files individually instead".formatted(legacyName));
            } else {
                logger.info("Found legacy folder '%s', importing missing data into '%s'...".formatted(legacyName, target.getName()));
            }

            var copied = copyMissing(legacy, target, logger);

            if (copied == 0) {
                logger.info("Nothing to import, all files already exist in '%s'".formatted(target.getName()));
            } else {
                logger.info("Imported %s file(s) from the legacy folder".formatted(copied));
            }

            return true;
        }

        return false;
    }

    private static int copyMissing(File from, File to, Logger logger) {
        if (!to.isDirectory() && !to.mkdirs()) {
            logger.error("Failed to create the data folder '%s', cannot import legacy data".formatted(to.getName()));
            return 0;
        }

        var copied = 0;

        for (String name : LEGACY_FILES) {
            if (copyIfMissing(new File(from, name), new File(to, name), logger)) {
                copied++;
            }
        }

        // SQLite keeps its data in regular files next to the configuration.
        var databases = from.listFiles((dir, name) -> name.endsWith(".db") || name.endsWith(".db-wal") || name.endsWith(".db-shm"));

        if (databases != null) {
            for (File database : databases) {
                if (copyIfMissing(database, new File(to, database.getName()), logger)) {
                    copied++;
                }
            }
        }

        return copied;
    }

    private static boolean copyIfMissing(File source, File target, Logger logger) {
        if (!source.isFile() || target.exists()) {
            return false;
        }

        try {
            Files.copy(source.toPath(), target.toPath());
            logger.info("Imported '%s'".formatted(source.getName()));
            return true;
        } catch (IOException e) {
            logger.error("Failed to import '%s', please copy it manually".formatted(source.getName()));
            e.printStackTrace();
            return false;
        }
    }
}
