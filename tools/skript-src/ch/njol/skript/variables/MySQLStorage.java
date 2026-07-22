/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lib.PatPeter.SQLibrary.Database
 *  lib.PatPeter.SQLibrary.MySQL
 */
package ch.njol.skript.variables;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.variables.SQLStorage;
import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.MySQL;

public class MySQLStorage
extends SQLStorage {
    MySQLStorage(String type) {
        super(type, "CREATE TABLE IF NOT EXISTS %s (rowid        BIGINT  NOT NULL  AUTO_INCREMENT  PRIMARY KEY,name         VARCHAR(380)  NOT NULL  UNIQUE,type         VARCHAR(50),value        BLOB(10000),update_guid  CHAR(36)  NOT NULL) CHARACTER SET ucs2 COLLATE ucs2_bin");
    }

    @Override
    public Database initialize(SectionNode config) {
        String host = this.getValue(config, "host");
        Integer port = this.getValue(config, "port", Integer.class);
        String user = this.getValue(config, "user");
        String password = this.getValue(config, "password");
        String database = this.getValue(config, "database");
        this.setTableName(config.get("table", "variables21"));
        if (host == null || port == null || user == null || password == null || database == null) {
            return null;
        }
        return new MySQL(SkriptLogger.LOGGER, "[Skript]", host, port.intValue(), database, user, password);
    }

    @Override
    protected boolean requiresFile() {
        return false;
    }
}

