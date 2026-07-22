/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lib.PatPeter.SQLibrary.Database
 *  lib.PatPeter.SQLibrary.DatabaseException
 *  lib.PatPeter.SQLibrary.SQLibrary
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.variables;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Task;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.variables.Variables;
import ch.njol.skript.variables.VariablesStorage;
import ch.njol.util.SynchronizedReference;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Callable;
import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.DatabaseException;
import lib.PatPeter.SQLibrary.SQLibrary;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class SQLStorage
extends VariablesStorage {
    public static final int MAX_VARIABLE_NAME_LENGTH = 380;
    public static final int MAX_CLASS_CODENAME_LENGTH = 50;
    public static final int MAX_VALUE_SIZE = 10000;
    private static final String SELECT_ORDER = "name, type, value, rowid";
    private static final String OLD_TABLE_NAME = "variables";
    @Nullable
    private String formattedCreateQuery;
    private final String createTableQuery;
    private String tableName;
    final SynchronizedReference<Database> db = new SynchronizedReference<Object>(null);
    private boolean monitor = false;
    long monitor_interval;
    private static final String guid = UUID.randomUUID().toString();
    private static final long TRANSACTION_DELAY = 500L;
    @Nullable
    private PreparedStatement writeQuery;
    @Nullable
    private PreparedStatement deleteQuery;
    @Nullable
    private PreparedStatement monitorQuery;
    @Nullable
    PreparedStatement monitorCleanUpQuery;
    long lastRowID = -1L;

    public SQLStorage(String type, String createTableQuery) {
        super(type);
        this.createTableQuery = createTableQuery;
        this.tableName = "variables21";
    }

    public String getTableName() {
        return this.tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Nullable
    public abstract Database initialize(SectionNode var1);

    @Nullable
    public String getFormattedCreateQuery() {
        if (this.formattedCreateQuery == null) {
            this.formattedCreateQuery = String.format(this.createTableQuery, this.tableName);
        }
        return this.formattedCreateQuery;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected boolean load_i(SectionNode n) {
        SynchronizedReference<Database> synchronizedReference = this.db;
        synchronized (synchronizedReference) {
            Database db;
            Plugin plugin = Bukkit.getPluginManager().getPlugin("SQLibrary");
            if (plugin == null || !(plugin instanceof SQLibrary)) {
                Skript.error("You need the plugin SQLibrary in order to use a database with Skript. You can download the latest version from https://dev.bukkit.org/projects/sqlibrary/files/");
                return false;
            }
            Boolean monitor_changes = this.getValue(n, "monitor changes", Boolean.class);
            Timespan monitor_interval = this.getValue(n, "monitor interval", Timespan.class);
            if (monitor_changes == null || monitor_interval == null) {
                return false;
            }
            this.monitor = monitor_changes;
            this.monitor_interval = monitor_interval.getAs(Timespan.TimePeriod.MILLISECOND);
            try {
                Database database = this.initialize(n);
                if (database == null) {
                    return false;
                }
                db = database;
                this.db.set(db);
            }
            catch (RuntimeException e) {
                if (e instanceof DatabaseException) {
                    Skript.error(e.getLocalizedMessage());
                    return false;
                }
                throw e;
            }
            SkriptLogger.setNode(null);
            if (!this.connect(true)) {
                return false;
            }
            try {
                boolean hasOldTable = false;
                boolean hadNewTable = db.isTable(this.getTableName());
                if (this.getFormattedCreateQuery() == null) {
                    Skript.error("Could not create the variables table in the database. The query to create the variables table '" + this.tableName + "' in the database '" + this.getUserConfigurationName() + "' is null.");
                    return false;
                }
                try {
                    db.query(this.getFormattedCreateQuery());
                }
                catch (SQLException e) {
                    Skript.error("Could not create the variables table '" + this.tableName + "' in the database '" + this.getUserConfigurationName() + "': " + e.getLocalizedMessage() + ". Please create the table yourself using the following query: " + String.format(this.createTableQuery, this.tableName).replace(",", ", ").replaceAll("\\s+", " "));
                    return false;
                }
                if (!this.prepareQueries()) {
                    return false;
                }
                ResultSet r2 = db.query("SELECT name, type, value, rowid FROM " + this.getTableName());
                assert (r2 != null);
                try {
                    this.loadVariables(r2);
                }
                finally {
                    r2.close();
                }
            }
            catch (SQLException e) {
                this.sqlException(e);
                return false;
            }
            Skript.newThread(new Runnable(){

                /*
                 * WARNING - Removed try catching itself - possible behaviour change.
                 */
                @Override
                public void run() {
                    while (!SQLStorage.this.closed) {
                        SynchronizedReference<Database> synchronizedReference = SQLStorage.this.db;
                        synchronized (synchronizedReference) {
                            try {
                                Database db = SQLStorage.this.db.get();
                                if (db != null) {
                                    db.query("SELECT * FROM " + SQLStorage.this.getTableName() + " LIMIT 1");
                                }
                            }
                            catch (SQLException sQLException) {
                                // empty catch block
                            }
                        }
                        try {
                            Thread.sleep(10000L);
                        }
                        catch (InterruptedException interruptedException) {}
                    }
                }
            }, "Skript database '" + this.getUserConfigurationName() + "' connection keep-alive thread").start();
            return true;
        }
    }

    @Override
    protected void allLoaded() {
        Skript.debug("Database " + this.getUserConfigurationName() + " loaded. Queue size = " + this.changesQueue.size());
        Skript.newThread(new Runnable(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                while (!SQLStorage.this.closed) {
                    long lastCommit;
                    SynchronizedReference<Database> synchronizedReference = SQLStorage.this.db;
                    synchronized (synchronizedReference) {
                        Database db = SQLStorage.this.db.get();
                        try {
                            if (db != null) {
                                db.getConnection().commit();
                            }
                        }
                        catch (SQLException e) {
                            SQLStorage.this.sqlException(e);
                        }
                        lastCommit = System.currentTimeMillis();
                    }
                    try {
                        Thread.sleep(Math.max(0L, lastCommit + 500L - System.currentTimeMillis()));
                    }
                    catch (InterruptedException interruptedException) {}
                }
            }
        }, "Skript database '" + this.getUserConfigurationName() + "' transaction committing thread").start();
        if (this.monitor) {
            Skript.newThread(new Runnable(){

                @Override
                public void run() {
                    try {
                        Thread.sleep(SQLStorage.this.monitor_interval);
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                    long lastWarning = Long.MIN_VALUE;
                    int WARING_INTERVAL = 10;
                    while (!SQLStorage.this.closed) {
                        long next = System.currentTimeMillis() + SQLStorage.this.monitor_interval;
                        SQLStorage.this.checkDatabase();
                        long now = System.currentTimeMillis();
                        if (next < now && lastWarning + 10000L < now) {
                            Skript.warning("Cannot load variables from the database fast enough (loading took " + (double)(now - next + SQLStorage.this.monitor_interval) / 1000.0 + "s, monitor interval = " + (double)SQLStorage.this.monitor_interval / 1000.0 + "s). Please increase your monitor interval or reduce usage of variables. (this warning will be repeated at most once every 10 seconds)");
                            lastWarning = now;
                        }
                        while (System.currentTimeMillis() < next) {
                            try {
                                Thread.sleep(next - System.currentTimeMillis());
                            }
                            catch (InterruptedException interruptedException) {}
                        }
                    }
                }
            }, "Skript database '" + this.getUserConfigurationName() + "' monitor thread").start();
        }
    }

    @Override
    protected File getFile(String file) {
        if (!((String)file).endsWith(".db")) {
            file = (String)file + ".db";
        }
        return new File((String)file);
    }

    @Override
    protected boolean connect() {
        return this.connect(false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private final boolean connect(boolean first) {
        SynchronizedReference<Database> synchronizedReference = this.db;
        synchronized (synchronizedReference) {
            Database db = this.db.get();
            if (db == null || !db.open()) {
                if (first) {
                    Skript.error("Cannot connect to the database '" + this.getUserConfigurationName() + "'! Please make sure that all settings are correct");
                } else {
                    Skript.exception("Cannot reconnect to the database '" + this.getUserConfigurationName() + "'!");
                }
                return false;
            }
            try {
                db.getConnection().setAutoCommit(false);
            }
            catch (SQLException e) {
                this.sqlException(e);
                return false;
            }
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean prepareQueries() {
        SynchronizedReference<Database> synchronizedReference = this.db;
        synchronized (synchronizedReference) {
            Database db = this.db.get();
            assert (db != null);
            try {
                try {
                    if (this.writeQuery != null) {
                        this.writeQuery.close();
                    }
                }
                catch (SQLException sQLException) {
                    // empty catch block
                }
                this.writeQuery = db.prepare("REPLACE INTO " + this.getTableName() + " (name, type, value, update_guid) VALUES (?, ?, ?, ?)");
                try {
                    if (this.deleteQuery != null) {
                        this.deleteQuery.close();
                    }
                }
                catch (SQLException sQLException) {
                    // empty catch block
                }
                this.deleteQuery = db.prepare("DELETE FROM " + this.getTableName() + " WHERE name = ?");
                try {
                    if (this.monitorQuery != null) {
                        this.monitorQuery.close();
                    }
                }
                catch (SQLException sQLException) {
                    // empty catch block
                }
                this.monitorQuery = db.prepare("SELECT name, type, value, rowid FROM " + this.getTableName() + " WHERE rowid > ? AND update_guid != ?");
                try {
                    if (this.monitorCleanUpQuery != null) {
                        this.monitorCleanUpQuery.close();
                    }
                }
                catch (SQLException sQLException) {
                    // empty catch block
                }
                this.monitorCleanUpQuery = db.prepare("DELETE FROM " + this.getTableName() + " WHERE value IS NULL AND rowid < ?");
            }
            catch (SQLException e) {
                Skript.exception((Throwable)e, "Could not prepare queries for the database '" + this.getUserConfigurationName() + "': " + e.getLocalizedMessage());
                return false;
            }
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void disconnect() {
        SynchronizedReference<Database> synchronizedReference = this.db;
        synchronized (synchronizedReference) {
            Database db = this.db.get();
            if (db != null) {
                db.close();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected boolean save(String name, @Nullable String type, @Nullable byte[] value) {
        SynchronizedReference<Database> synchronizedReference = this.db;
        synchronized (synchronizedReference) {
            if (name.length() > 380) {
                Skript.error("The name of the variable {" + name + "} is too long to be saved in a database (length: " + name.length() + ", maximum allowed: 380)! It will be truncated and won't bet available under the same name again when loaded.");
            }
            if (value != null && value.length > 10000) {
                Skript.error("The variable {" + name + "} cannot be saved in the database as its value's size (" + value.length + ") exceeds the maximum allowed size of 10000! An attempt to save the variable will be made nonetheless.");
            }
            try {
                if (type == null) {
                    assert (value == null);
                    PreparedStatement deleteQuery = this.deleteQuery;
                    assert (deleteQuery != null);
                    deleteQuery.setString(1, name);
                    deleteQuery.executeUpdate();
                } else {
                    int i = 1;
                    PreparedStatement writeQuery = this.writeQuery;
                    assert (writeQuery != null);
                    writeQuery.setString(i++, name);
                    writeQuery.setString(i++, type);
                    writeQuery.setBytes(i++, value);
                    writeQuery.setString(i++, guid);
                    writeQuery.executeUpdate();
                }
            }
            catch (SQLException e) {
                this.sqlException(e);
                return false;
            }
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void close() {
        SynchronizedReference<Database> synchronizedReference = this.db;
        synchronized (synchronizedReference) {
            super.close();
            Database db = this.db.get();
            if (db != null) {
                try {
                    db.getConnection().commit();
                }
                catch (SQLException e) {
                    this.sqlException(e);
                }
                db.close();
                this.db.set(null);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void checkDatabase() {
        try {
            long lastRowID;
            try (ResultSet r = null;){
                SynchronizedReference<Database> synchronizedReference = this.db;
                synchronized (synchronizedReference) {
                    block15: {
                        if (!this.closed && this.db.get() != null) break block15;
                        return;
                    }
                    lastRowID = this.lastRowID;
                    PreparedStatement monitorQuery = this.monitorQuery;
                    assert (monitorQuery != null);
                    monitorQuery.setLong(1, lastRowID);
                    monitorQuery.setString(2, guid);
                    monitorQuery.execute();
                    r = monitorQuery.getResultSet();
                    assert (r != null);
                }
                if (!this.closed) {
                    this.loadVariables(r);
                }
            }
            if (!this.closed) {
                new Task(this, (Plugin)Skript.getInstance(), (long)Math.ceil(2.0 * (double)this.monitor_interval / 50.0) + 100L, true){
                    final /* synthetic */ SQLStorage this$0;
                    {
                        this.this$0 = this$0;
                        super(plugin, delay, async);
                    }

                    /*
                     * WARNING - Removed try catching itself - possible behaviour change.
                     */
                    @Override
                    public void run() {
                        try {
                            SynchronizedReference<Database> synchronizedReference = this.this$0.db;
                            synchronized (synchronizedReference) {
                                if (this.this$0.closed || this.this$0.db.get() == null) {
                                    return;
                                }
                                PreparedStatement monitorCleanUpQuery = this.this$0.monitorCleanUpQuery;
                                assert (monitorCleanUpQuery != null);
                                monitorCleanUpQuery.setLong(1, lastRowID);
                                monitorCleanUpQuery.executeUpdate();
                            }
                        }
                        catch (SQLException e) {
                            this.this$0.sqlException(e);
                        }
                    }
                };
            }
        }
        catch (SQLException e) {
            this.sqlException(e);
        }
    }

    private void loadVariables(final ResultSet r) throws SQLException {
        SQLException e = Task.callSync(new Callable<SQLException>(){
            final /* synthetic */ SQLStorage this$0;
            {
                this.this$0 = this$0;
            }

            @Override
            @Nullable
            public SQLException call() throws Exception {
                try {
                    while (r.next()) {
                        Serializer<?> s;
                        String name;
                        int i = 1;
                        if ((name = r.getString(i++)) == null) {
                            Skript.error("Variable with NULL name found in the database '" + this.this$0.getUserConfigurationName() + "', ignoring it");
                            continue;
                        }
                        String type = r.getString(i++);
                        byte[] value = r.getBytes(i++);
                        this.this$0.lastRowID = r.getLong(i++);
                        if (value == null) {
                            Variables.variableLoaded(name, null, this.this$0);
                            continue;
                        }
                        ClassInfo<?> c = Classes.getClassInfoNoError(type);
                        if (c == null || (s = c.getSerializer()) == null) {
                            Skript.error("Cannot load the variable {" + name + "} from the database '" + this.this$0.getUserConfigurationName() + "', because the type '" + type + "' cannot be recognised or cannot be stored in variables");
                            continue;
                        }
                        Object d = Classes.deserialize(c, value);
                        if (d == null) {
                            Skript.error("Cannot load the variable {" + name + "} from the database '" + this.this$0.getUserConfigurationName() + "', because it cannot be loaded as " + c.getName().withIndefiniteArticle());
                            continue;
                        }
                        Variables.variableLoaded(name, d, this.this$0);
                    }
                }
                catch (SQLException e) {
                    return e;
                }
                return null;
            }
        });
        if (e != null) {
            throw e;
        }
    }

    void sqlException(SQLException e) {
        Skript.error("database error: " + e.getLocalizedMessage());
        if (Skript.testing()) {
            e.printStackTrace();
        }
        this.prepareQueries();
    }
}

