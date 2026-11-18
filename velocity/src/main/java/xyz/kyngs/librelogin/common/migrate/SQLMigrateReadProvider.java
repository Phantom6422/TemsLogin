/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package xyz.kyngs.librelogin.common.migrate;

import xyz.kyngs.librelogin.api.Logger;
import xyz.kyngs.librelogin.api.database.connector.SQLDatabaseConnector;

import java.util.regex.Pattern;

public abstract class SQLMigrateReadProvider extends MigrateReadProvider {

    /**
     * Table names are interpolated into SQL statements, so they must not contain user controlled SQL.
     */
    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?$");

    protected final String tableName;
    protected final Logger logger;
    protected final SQLDatabaseConnector connector;

    public SQLMigrateReadProvider(String tableName, Logger logger, SQLDatabaseConnector connector) {
        if (tableName == null || !SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Invalid table name '%s', only letters, digits and underscores are allowed".formatted(tableName));
        }

        this.tableName = tableName;
        this.logger = logger;
        this.connector = connector;
    }
}
