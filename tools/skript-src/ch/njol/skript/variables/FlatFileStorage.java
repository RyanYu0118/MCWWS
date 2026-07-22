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
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.ExceptionUtils;
import ch.njol.skript.util.FileUtils;
import ch.njol.skript.util.Task;
import ch.njol.skript.util.Version;
import ch.njol.skript.variables.SerializedVariable;
import ch.njol.skript.variables.Variables;
import ch.njol.skript.variables.VariablesStorage;
import ch.njol.util.NotifyingReference;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class FlatFileStorage
extends VariablesStorage {
    public static final Charset FILE_CHARSET = StandardCharsets.UTF_8;
    private static final long SAVE_TASK_DELAY = 6000L;
    private static final long SAVE_TASK_PERIOD = 6000L;
    private final NotifyingReference<PrintWriter> changesWriter = new NotifyingReference();
    private volatile boolean loaded = false;
    private static int REQUIRED_CHANGES_FOR_RESAVE = 1000;
    private final AtomicInteger changes = new AtomicInteger(0);
    @Nullable
    private Task saveTask;
    private boolean loadError = false;
    private static final Pattern CSV_LINE_PATTERN = Pattern.compile("(?<=^|,)\\s*(?:([^\",]*)|\"((?:[^\"]+|\"\")*)\")\\s*(?:,|$)");
    private static final Pattern CONTAINS_WHITESPACE = Pattern.compile("\\s");

    FlatFileStorage(String type) {
        super(type);
    }

    @Override
    protected boolean load_i(SectionNode sectionNode) {
        SkriptLogger.setNode(null);
        if (this.file == null) {
            assert (false) : this;
            return false;
        }
        IOException ioException = null;
        int unsuccessfulVariableCount = 0;
        StringBuilder invalid = new StringBuilder();
        Version v2_1 = new Version(2, 1);
        boolean update2_1 = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(this.file.toPath(), new OpenOption[0]), FILE_CHARSET));){
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                ++lineNum;
                if ((line = line.trim()).isEmpty() || line.startsWith("#")) {
                    if (!line.startsWith("# version:")) continue;
                    try {
                        Version csvSkriptVersion = new Version(line.substring("# version:".length()).trim());
                        update2_1 = csvSkriptVersion.isSmallerThan(v2_1);
                    }
                    catch (IllegalArgumentException illegalArgumentException) {}
                    continue;
                }
                String[] split = FlatFileStorage.splitCSV(line);
                if (split == null || split.length != 3) {
                    Skript.error("invalid amount of commas in line " + lineNum + " ('" + line + "')");
                    if (invalid.length() != 0) {
                        invalid.append(", ");
                    }
                    invalid.append(split == null ? "<unknown>" : split[0]);
                    ++unsuccessfulVariableCount;
                    continue;
                }
                if (split[1].equals("null")) {
                    Variables.variableLoaded(split[0], null, this);
                    continue;
                }
                Object deserializedValue = update2_1 ? Classes.deserialize(split[1], split[2]) : Classes.deserialize(split[1], FlatFileStorage.decode(split[2]));
                if (deserializedValue == null) {
                    if (invalid.length() != 0) {
                        invalid.append(", ");
                    }
                    invalid.append(split[0]);
                    ++unsuccessfulVariableCount;
                    continue;
                }
                Variables.variableLoaded(split[0], deserializedValue, this);
            }
        }
        catch (IOException e) {
            this.loadError = true;
            ioException = e;
        }
        if (ioException != null || unsuccessfulVariableCount > 0 || update2_1) {
            if (unsuccessfulVariableCount > 0) {
                Skript.error(unsuccessfulVariableCount + " variable" + (unsuccessfulVariableCount == 1 ? "" : "s") + " could not be loaded!");
                Skript.error("Affected variables: " + invalid.toString());
            }
            if (ioException != null) {
                Skript.error("An I/O error occurred while loading the variables: " + ExceptionUtils.toString(ioException));
                Skript.error("This means that some to all variables could not be loaded!");
            }
            try {
                if (update2_1) {
                    Skript.info("[2.1] updating " + this.file.getName() + " to the new format...");
                }
                File backupFile = FileUtils.backup(this.file);
                Skript.info("Created a backup of " + this.file.getName() + " as " + backupFile.getName());
                this.loadError = false;
            }
            catch (IOException ex) {
                Skript.error("Could not backup " + this.file.getName() + ": " + ex.getMessage());
            }
        }
        if (update2_1) {
            this.saveVariables(false);
            Skript.info(this.file.getName() + " successfully updated.");
        }
        this.connect();
        this.saveTask = new Task((Plugin)Skript.getInstance(), 6000L, 6000L, true){

            @Override
            public void run() {
                if (FlatFileStorage.this.changes.get() >= REQUIRED_CHANGES_FOR_RESAVE) {
                    FlatFileStorage.this.saveVariables(false);
                    FlatFileStorage.this.changes.set(0);
                }
            }
        };
        return ioException == null;
    }

    @Override
    protected void allLoaded() {
    }

    @Override
    protected boolean requiresFile() {
        return true;
    }

    @Override
    protected File getFile(String fileName) {
        return new File(fileName);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected final void disconnect() {
        Object object = this.connectionLock;
        synchronized (object) {
            this.clearChangesQueue();
            NotifyingReference<PrintWriter> notifyingReference = this.changesWriter;
            synchronized (notifyingReference) {
                PrintWriter printWriter = this.changesWriter.get();
                if (printWriter != null) {
                    printWriter.close();
                    this.changesWriter.set(null);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected final boolean connect() {
        Object object = this.connectionLock;
        synchronized (object) {
            NotifyingReference<PrintWriter> notifyingReference = this.changesWriter;
            synchronized (notifyingReference) {
                boolean bl;
                assert (this.file != null);
                if (this.changesWriter.get() != null) {
                    return true;
                }
                FileOutputStream fos = new FileOutputStream(this.file, true);
                try {
                    this.changesWriter.set(new PrintWriter(new OutputStreamWriter((OutputStream)fos, FILE_CHARSET)));
                    this.loaded = true;
                    bl = true;
                }
                catch (Throwable throwable) {
                    try {
                        try {
                            fos.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                        throw throwable;
                    }
                    catch (IOException e) {
                        Skript.exception((Throwable)e, new String[0]);
                        return false;
                    }
                }
                fos.close();
                return bl;
            }
        }
    }

    @Override
    public void close() {
        this.clearChangesQueue();
        super.close();
        this.saveVariables(true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected boolean save(String name, @Nullable String type, @Nullable byte[] value) {
        Object object = this.connectionLock;
        synchronized (object) {
            NotifyingReference<PrintWriter> notifyingReference = this.changesWriter;
            synchronized (notifyingReference) {
                PrintWriter printWriter;
                if (!this.loaded && type == null) {
                    return true;
                }
                while ((printWriter = this.changesWriter.get()) == null) {
                    try {
                        this.changesWriter.wait();
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                FlatFileStorage.writeCSV(printWriter, name, type, value == null ? "" : FlatFileStorage.encode(value));
                printWriter.flush();
                this.changes.incrementAndGet();
            }
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Converted monitor instructions to comments
     * Lifted jumps to return sites
     */
    public final void saveVariables(boolean finalSave) {
        block41: {
            block40: {
                block38: {
                    block39: {
                        if (finalSave) {
                            if (this.saveTask != null) {
                                this.saveTask.cancel();
                            }
                            if (this.backupTask != null) {
                                this.backupTask.cancel();
                            }
                        }
                        Variables.getReadLock().lock();
                        Object object = this.connectionLock;
                        // MONITORENTER : object
                        if (this.file != null) break block38;
                        assert (false) : this;
                        if (finalSave) break block39;
                        this.connect();
                    }
                    // MONITOREXIT : object
                    Variables.getReadLock().unlock();
                    boolean gotWriteLock = Variables.variablesLock.writeLock().tryLock();
                    if (!gotWriteLock) return;
                    try {
                        Variables.processChangeQueue();
                        return;
                    }
                    finally {
                        Variables.variablesLock.writeLock().unlock();
                    }
                }
                this.disconnect();
                if (!this.loadError) break block40;
                try {
                    File backup = FileUtils.backup(this.file);
                    Skript.info("Created a backup of the old " + this.file.getName() + " as " + backup.getName());
                    this.loadError = false;
                }
                catch (IOException e) {
                    Skript.error("Could not backup the old " + this.file.getName() + ": " + ExceptionUtils.toString(e));
                    Skript.error("No variables are saved!");
                    if (!finalSave) {
                        this.connect();
                    }
                    // MONITOREXIT : object
                    Variables.getReadLock().unlock();
                    boolean gotWriteLock = Variables.variablesLock.writeLock().tryLock();
                    if (!gotWriteLock) return;
                    try {
                        Variables.processChangeQueue();
                        return;
                    }
                    finally {
                        Variables.variablesLock.writeLock().unlock();
                    }
                }
            }
            try {
                File tempFile = new File(this.file.getParentFile(), this.file.getName() + ".temp");
                try (PrintWriter pw = new PrintWriter(tempFile, "UTF-8");){
                    pw.println("# === Skript's variable storage ===");
                    pw.println("# Please do not modify this file manually!");
                    pw.println("#");
                    pw.println("# version: " + String.valueOf(Skript.getVersion()));
                    pw.println();
                    this.save(pw, "", Variables.getVariables());
                    pw.println();
                    pw.flush();
                    pw.close();
                    FileUtils.move(tempFile, this.file, true);
                    return;
                }
                catch (IOException e) {
                    Skript.error("Unable to make a final save of the database '" + this.getUserConfigurationName() + "' (no variables are lost): " + ExceptionUtils.toString(e));
                    return;
                }
            }
            finally {
                if (finalSave) break block41;
                this.connect();
            }
        }
        // MONITOREXIT : object
        return;
    }

    private void save(PrintWriter pw, String parent, TreeMap<String, Object> map) {
        if (parent.startsWith("-")) {
            return;
        }
        block2: for (Map.Entry<String, Object> childEntry : map.entrySet()) {
            String name;
            Object childNode = childEntry.getValue();
            String childKey = childEntry.getKey();
            if (childNode == null) continue;
            if (childNode instanceof TreeMap) {
                this.save(pw, parent + childKey + "::", (TreeMap)childNode);
                continue;
            }
            String string = name = childKey == null ? parent.substring(0, parent.length() - "::".length()) : parent + childKey;
            if (name.startsWith("-")) continue;
            try {
                for (VariablesStorage storage : Variables.STORAGES) {
                    SerializedVariable.Value serializedValue;
                    if (!storage.accept(name)) continue;
                    if (storage != this || (serializedValue = Classes.serialize(childNode)) == null) continue block2;
                    FlatFileStorage.writeCSV(pw, name, serializedValue.type, FlatFileStorage.encode(serializedValue.data));
                }
            }
            catch (Exception ex) {
                Skript.exception((Throwable)ex, "Error saving variable named " + name);
            }
        }
    }

    static String encode(byte[] data) {
        char[] encoded = new char[data.length * 2];
        for (int i = 0; i < data.length; ++i) {
            encoded[2 * i] = Character.toUpperCase(Character.forDigit((data[i] & 0xF0) >>> 4, 16));
            encoded[2 * i + 1] = Character.toUpperCase(Character.forDigit(data[i] & 0xF, 16));
        }
        return new String(encoded);
    }

    static byte[] decode(String hex) {
        byte[] decoded = new byte[hex.length() / 2];
        for (int i = 0; i < decoded.length; ++i) {
            decoded[i] = (byte)((Character.digit(hex.charAt(2 * i), 16) << 4) + Character.digit(hex.charAt(2 * i + 1), 16));
        }
        return decoded;
    }

    @Nullable
    static String[] splitCSV(String line) {
        Matcher matcher = CSV_LINE_PATTERN.matcher(line);
        int lastEnd = 0;
        ArrayList<String> result = new ArrayList<String>();
        while (matcher.find()) {
            if (lastEnd != matcher.start()) {
                return null;
            }
            if (matcher.group(1) != null) {
                result.add(matcher.group(1).trim());
            } else {
                result.add(matcher.group(2).replace("\"\"", "\""));
            }
            lastEnd = matcher.end();
        }
        if (lastEnd != line.length()) {
            return null;
        }
        return result.toArray(new String[0]);
    }

    private static void writeCSV(PrintWriter printWriter, String ... values) {
        assert (values.length == 3);
        for (int i = 0; i < values.length; ++i) {
            Object value;
            boolean escapingNeeded;
            if (i != 0) {
                printWriter.print(", ");
            }
            boolean bl = escapingNeeded = (value = values[i]) != null && (((String)value).contains(",") || ((String)value).contains("\"") || ((String)value).contains("#") || CONTAINS_WHITESPACE.matcher((CharSequence)value).find());
            if (escapingNeeded) {
                value = "\"" + ((String)value).replace("\"", "\"\"") + "\"";
            }
            printWriter.print((String)value);
        }
        printWriter.println();
    }

    public static void setRequiredChangesForResave(int value) {
        if (value <= 0) {
            Skript.warning("Variable changes until save cannot be zero or less. Using default of 1000.");
            value = 1000;
        }
        REQUIRED_CHANGES_FOR_RESAVE = value;
    }
}

