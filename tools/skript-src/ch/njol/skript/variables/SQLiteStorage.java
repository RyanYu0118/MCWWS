/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lib.PatPeter.SQLibrary.Database
 *  lib.PatPeter.SQLibrary.SQLite
 */
package ch.njol.skript.variables;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.variables.SQLStorage;
import java.io.File;
import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.SQLite;

public class SQLiteStorage
extends SQLStorage {
    SQLiteStorage(String type) {
        super(type, "CREATE TABLE IF NOT EXISTS %s (name         VARCHAR(380)  NOT NULL  PRIMARY KEY,type         VARCHAR(50),value        BLOB(10000),update_guid  CHAR(36)  NOT NULL)");
    }

    @Override
    public Database initialize(SectionNode config) {
        File f = this.file;
        if (f == null) {
            return null;
        }
        this.setTableName(config.get("table", "variables21"));
        String name = f.getName();
        assert (name.endsWith(".db"));
        return new SQLite(SkriptLogger.LOGGER, "[Skript]", f.getParent(), name.substring(0, name.length() - ".db".length()));
    }

    @Override
    protected boolean requiresFile() {
        return true;
    }
}

