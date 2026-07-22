/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.variables;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.FileUtils;
import ch.njol.skript.util.Task;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.variables.SerializedVariable;
import ch.njol.util.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class VariablesStorage
implements Closeable {
    private static final int QUEUE_SIZE = 1000;
    private static final int FIRST_WARNING = 300;
    final LinkedBlockingQueue<SerializedVariable> changesQueue = new LinkedBlockingQueue(1000);
    protected volatile boolean closed = false;
    private String databaseName;
    private final String databaseType;
    @Nullable
    protected File file;
    @Nullable
    private Pattern variableNamePattern;
    private final Thread writeThread;
    private static final Set<File> registeredFiles = new HashSet<File>();
    protected final Object connectionLock = new Object();
    @Nullable
    protected Task backupTask = null;
    private static final int WARNING_INTERVAL = 10;
    private static final int ERROR_INTERVAL = 10;
    private long lastWarning = Long.MIN_VALUE;
    private long lastError = Long.MIN_VALUE;

    protected VariablesStorage(String type) {
        assert (type != null);
        this.databaseType = type;
        this.writeThread = Skript.newThread(() -> {
            while (!this.closed) {
                try {
                    SerializedVariable variable = this.changesQueue.take();
                    SerializedVariable.Value value = variable.value;
                    if (value != null) {
                        this.save(variable.name, value.type, value.data);
                        continue;
                    }
                    this.save(variable.name, null, null);
                }
                catch (InterruptedException interruptedException) {}
            }
        }, "Skript variable save thread for database '" + type + "'");
    }

    protected final String getUserConfigurationName() {
        return this.databaseName;
    }

    protected final String getDatabaseType() {
        return this.databaseType;
    }

    @Nullable
    protected String getValue(SectionNode sectionNode, String key) {
        return this.getValue(sectionNode, key, String.class);
    }

    @Nullable
    protected <T> T getValue(SectionNode sectionNode, String key, Class<T> type) {
        String rawValue = sectionNode.getValue(key);
        if (rawValue == null) {
            Skript.error("The config is missing the entry for '" + key + "' in the database '" + this.databaseName + "'");
            return null;
        }
        try (ParseLogHandler log = SkriptLogger.startParseLogHandler();){
            T parsedValue = Classes.parse(rawValue, type, ParseContext.CONFIG);
            if (parsedValue == null) {
                log.printError("The entry for '" + key + "' in the database '" + this.databaseName + "' must be " + Classes.getSuperClassInfo(type).getName().withIndefiniteArticle());
            } else {
                log.printLog();
            }
            T t = parsedValue;
            return t;
        }
    }

    public final boolean load(SectionNode sectionNode) {
        this.databaseName = sectionNode.getKey();
        String pattern = this.getValue(sectionNode, "pattern");
        if (pattern == null) {
            return false;
        }
        try {
            this.variableNamePattern = pattern.equals(".*") || pattern.equals(".+") ? null : Pattern.compile(pattern);
        }
        catch (PatternSyntaxException e) {
            Skript.error("Invalid pattern '" + pattern + "': " + e.getLocalizedMessage());
            return false;
        }
        if (this.requiresFile()) {
            String fileName = this.getValue(sectionNode, "file");
            if (fileName == null) {
                return false;
            }
            this.file = this.getFile(fileName).getAbsoluteFile();
            if (this.file.exists() && !this.file.isFile()) {
                Skript.error("The database file '" + this.file.getName() + "' must be an actual file, not a directory.");
                return false;
            }
            try {
                this.file.createNewFile();
            }
            catch (IOException e) {
                Skript.error("Cannot create the database file '" + this.file.getName() + "': " + e.getLocalizedMessage());
                return false;
            }
            if (!this.file.canWrite()) {
                Skript.error("Cannot write to the database file '" + this.file.getName() + "'!");
                return false;
            }
            if (!this.file.canRead()) {
                Skript.error("Cannot read from the database file '" + this.file.getName() + "'!");
                return false;
            }
            if (registeredFiles.contains(this.file)) {
                Skript.error("Database `" + this.databaseName + "` failed to load. The file `" + fileName + "` is already registered to another database.");
                return false;
            }
            registeredFiles.add(this.file);
            if (!"0".equals(this.getValue(sectionNode, "backup interval"))) {
                Timespan backupInterval = this.getValue(sectionNode, "backup interval", Timespan.class);
                int toKeep = this.getValue(sectionNode, "backups to keep", Integer.class);
                boolean removeBackups = false;
                boolean startBackup = true;
                if (backupInterval != null) {
                    if (toKeep == 0) {
                        startBackup = false;
                    } else if (toKeep >= 1) {
                        removeBackups = true;
                    }
                }
                if (startBackup) {
                    this.startBackupTask(backupInterval, removeBackups, toKeep);
                } else {
                    try {
                        FileUtils.backupPurge(this.file, toKeep);
                    }
                    catch (IOException e) {
                        Skript.error("Variables backup wipe failed: " + e.getLocalizedMessage());
                    }
                }
            }
        }
        if (!this.load_i(sectionNode)) {
            return false;
        }
        this.writeThread.start();
        Skript.closeOnDisable(this);
        return true;
    }

    protected abstract boolean load_i(SectionNode var1);

    protected abstract void allLoaded();

    protected abstract boolean requiresFile();

    protected abstract File getFile(String var1);

    protected abstract boolean connect();

    protected abstract void disconnect();

    public void startBackupTask(Timespan backupInterval, final boolean removeBackups, final int toKeep) {
        if (this.file == null || backupInterval.getAs(Timespan.TimePeriod.TICK) == 0L) {
            return;
        }
        this.backupTask = new Task(this, (Plugin)Skript.getInstance(), backupInterval.getAs(Timespan.TimePeriod.TICK), backupInterval.getAs(Timespan.TimePeriod.TICK), true){
            final /* synthetic */ VariablesStorage this$0;
            {
                this.this$0 = this$0;
                super(plugin, delay, period, async);
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                Object object = this.this$0.connectionLock;
                synchronized (object) {
                    this.this$0.disconnect();
                    try {
                        FileUtils.backup(this.this$0.file);
                        if (removeBackups) {
                            try {
                                FileUtils.backupPurge(this.this$0.file, toKeep);
                            }
                            catch (IOException | IllegalArgumentException e) {
                                Skript.error("Automatic variables backup purge failed: " + e.getLocalizedMessage());
                            }
                        }
                    }
                    catch (IOException e) {
                        Skript.error("Automatic variables backup failed: " + e.getLocalizedMessage());
                    }
                    finally {
                        this.this$0.connect();
                    }
                }
            }
        };
    }

    boolean accept(@Nullable String var) {
        if (var == null) {
            return false;
        }
        return this.variableNamePattern == null || this.variableNamePattern.matcher(var).matches();
    }

    @Nullable
    public Pattern getNamePattern() {
        return this.variableNamePattern;
    }

    final void save(SerializedVariable var) {
        if (this.changesQueue.size() > 300 && this.lastWarning < System.currentTimeMillis() - 10000L) {
            Skript.warning("Cannot write variables to the database '" + this.databaseName + "' at sufficient speed; server performance may suffer and many variables will be lost if the server crashes. (this warning will be repeated at most once every 10 seconds)");
            this.lastWarning = System.currentTimeMillis();
        }
        if (!this.changesQueue.offer(var)) {
            if (this.lastError < System.currentTimeMillis() - 10000L) {
                Skript.error("Skript cannot save any variables to the database '" + this.databaseName + "'. The server will hang and may crash if no more variables can be saved.");
                this.lastError = System.currentTimeMillis();
            }
            while (true) {
                try {
                    this.changesQueue.put(var);
                }
                catch (InterruptedException interruptedException) {
                    continue;
                }
                break;
            }
        }
    }

    @Override
    public void close() {
        while (this.changesQueue.size() > 0) {
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException interruptedException) {}
        }
        this.closed = true;
        this.writeThread.interrupt();
    }

    protected void clearChangesQueue() {
        this.changesQueue.clear();
    }

    protected abstract boolean save(String var1, @Nullable String var2, @Nullable byte[] var3);
}

