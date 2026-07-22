/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.Multimap
 *  org.bukkit.Bukkit
 *  org.bukkit.configuration.serialization.ConfigurationSerializable
 *  org.bukkit.configuration.serialization.ConfigurationSerialization
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnmodifiableView
 */
package ch.njol.skript.variables;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.ConfigurationSerializer;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.FlatFileStorage;
import ch.njol.skript.variables.MySQLStorage;
import ch.njol.skript.variables.SQLiteStorage;
import ch.njol.skript.variables.SerializedVariable;
import ch.njol.skript.variables.VariablesMap;
import ch.njol.skript.variables.VariablesStorage;
import ch.njol.util.Kleenean;
import ch.njol.util.NonNullPair;
import ch.njol.util.Pair;
import ch.njol.util.StringUtils;
import ch.njol.util.SynchronizedReference;
import ch.njol.util.coll.iterator.EmptyIterator;
import ch.njol.yggdrasil.Yggdrasil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.skriptlang.skript.lang.converter.Converters;

public class Variables {
    public static final short YGGDRASIL_VERSION = 1;
    public static final Yggdrasil yggdrasil = new Yggdrasil(1);
    public static boolean caseInsensitiveVariables = true;
    private static final String CONFIGURATION_SERIALIZABLE_PREFIX = "ConfigurationSerializable_";
    private static final Multimap<Class<? extends VariablesStorage>, String> TYPES = HashMultimap.create();
    static final List<VariablesStorage> STORAGES;
    static final ReadWriteLock variablesLock;
    static final VariablesMap variables;
    private static final Map<Event, VariablesMap> localVariables;
    static final Queue<VariableChange> changeQueue;
    private static final SynchronizedReference<Map<String, NonNullPair<Object, VariablesStorage>>> TEMP_VARIABLES;
    private static final int MAX_CONFLICT_WARNINGS = 50;
    private static int loadConflicts;
    static final BlockingQueue<SerializedVariable> saveQueue;
    private static volatile boolean closed;
    private static final Thread saveThread;

    public static @UnmodifiableView List<VariablesStorage> getStores() {
        return Collections.unmodifiableList(STORAGES);
    }

    public static <T extends VariablesStorage> boolean registerStorage(Class<T> storage, String ... names) {
        if (TYPES.containsKey(storage)) {
            return false;
        }
        for (String name : names) {
            if (!TYPES.containsValue((Object)name.toLowerCase(Locale.ENGLISH))) continue;
            return false;
        }
        for (String name : names) {
            TYPES.put(storage, (Object)name.toLowerCase(Locale.ENGLISH));
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean load() {
        assert (Variables.variables.treeMap.isEmpty());
        assert (Variables.variables.hashMap.isEmpty());
        assert (STORAGES.isEmpty());
        Config config = SkriptConfig.getConfig();
        if (config == null) {
            throw new SkriptAPIException("Cannot load variables before the config");
        }
        Node databases = config.getMainNode().get("databases");
        if (!(databases instanceof SectionNode)) {
            Skript.error("The config is missing the required 'databases' section that defines where the variables are saved");
            return false;
        }
        Skript.closeOnDisable(Variables::close);
        Thread loadingLoggerThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(Skript.logNormal() ? 1000L : 5000L);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
                SynchronizedReference<Map<String, NonNullPair<Object, VariablesStorage>>> synchronizedReference = TEMP_VARIABLES;
                synchronized (synchronizedReference) {
                    Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
                    if (tvs == null) {
                        break;
                    }
                    Skript.info("Loaded " + tvs.size() + " variables so far...");
                }
            }
        });
        loadingLoggerThread.start();
        try {
            boolean successful = true;
            for (Node node : (SectionNode)databases) {
                if (node instanceof SectionNode) {
                    int newVariablesLoaded;
                    int totalVariablesLoaded;
                    VariablesStorage variablesStorage;
                    Object constructor;
                    SectionNode sectionNode = (SectionNode)node;
                    String type = sectionNode.getValue("type");
                    if (type == null) {
                        Skript.error("Missing entry 'type' in database definition");
                        successful = false;
                        continue;
                    }
                    String name = sectionNode.getKey();
                    assert (name != null);
                    Optional<Class> optional = TYPES.entries().stream().filter(entry -> ((String)entry.getValue()).equalsIgnoreCase(type)).map(Map.Entry::getKey).findFirst();
                    if (!optional.isPresent()) {
                        if (type.equalsIgnoreCase("disabled") || type.equalsIgnoreCase("none")) continue;
                        Skript.error("Invalid database type '" + type + "'");
                        successful = false;
                        continue;
                    }
                    try {
                        Class storageClass = optional.get();
                        constructor = storageClass.getDeclaredConstructor(String.class);
                        ((Constructor)constructor).setAccessible(true);
                        variablesStorage = (VariablesStorage)((Constructor)constructor).newInstance(type);
                    }
                    catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                        Skript.error("Failed to initialize database `" + name + "`");
                        successful = false;
                        continue;
                    }
                    constructor = TEMP_VARIABLES;
                    synchronized (constructor) {
                        Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
                        assert (tvs != null);
                        totalVariablesLoaded = tvs.size();
                    }
                    long start = System.currentTimeMillis();
                    if (Skript.logVeryHigh()) {
                        Skript.info("Loading database '" + node.getKey() + "'...");
                    }
                    if (variablesStorage.load(sectionNode)) {
                        STORAGES.add(variablesStorage);
                    } else {
                        successful = false;
                    }
                    SynchronizedReference<Map<String, NonNullPair<Object, VariablesStorage>>> synchronizedReference = TEMP_VARIABLES;
                    synchronized (synchronizedReference) {
                        Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
                        assert (tvs != null);
                        newVariablesLoaded = tvs.size() - totalVariablesLoaded;
                    }
                    if (!Skript.logVeryHigh()) continue;
                    Skript.info("Loaded " + newVariablesLoaded + " variables from the database '" + sectionNode.getKey() + "' in " + (double)((System.currentTimeMillis() - start) / 100L) / 10.0 + " seconds");
                    continue;
                }
                Skript.error("Invalid line in databases: databases must be defined as sections");
                successful = false;
            }
            if (!successful) {
                boolean bl = false;
                return bl;
            }
            if (STORAGES.isEmpty()) {
                Skript.error("No databases to store variables are defined. Please enable at least the default database, even if you don't use variables at all.");
                boolean bl = false;
                return bl;
            }
        }
        finally {
            SkriptLogger.setNode(null);
            int notStoredVariablesCount = Variables.onStoragesLoaded();
            if (notStoredVariablesCount != 0) {
                Skript.warning(notStoredVariablesCount + " variables were possibly discarded due to not belonging to any database (SQL databases keep such variables and will continue to generate this warning, while CSV discards them).");
            }
            loadingLoggerThread.interrupt();
            saveThread.start();
        }
        return true;
    }

    public static String[] splitVariableName(String name) {
        int index;
        String sep = "::";
        int sepLen = sep.length();
        int first = name.indexOf(sep);
        if (first == -1) {
            return new String[]{name};
        }
        int second = name.indexOf(sep, first + sepLen);
        if (second == -1) {
            return new String[]{name.substring(0, first), name.substring(first + sepLen)};
        }
        ArrayList<String> parts = new ArrayList<String>();
        parts.add(name.substring(0, first));
        parts.add(name.substring(first + sepLen, second));
        int start = second + sepLen;
        while ((index = name.indexOf(sep, start)) != -1) {
            parts.add(name.substring(start, index));
            start = index + sepLen;
        }
        parts.add(name.substring(start));
        return (String[])parts.toArray(String[]::new);
    }

    static TreeMap<String, Object> getVariables() {
        return Variables.variables.treeMap;
    }

    static Map<String, Object> getVariablesHashMap() {
        return Collections.unmodifiableMap(Variables.variables.hashMap);
    }

    static Lock getReadLock() {
        return variablesLock.readLock();
    }

    @Nullable
    public static VariablesMap removeLocals(Event event) {
        return localVariables.remove(event);
    }

    public static void setLocalVariables(Event event, @Nullable Object map) {
        if (map != null) {
            localVariables.put(event, (VariablesMap)map);
        } else {
            Variables.removeLocals(event);
        }
    }

    @Nullable
    public static Object copyLocalVariables(Event event) {
        VariablesMap from = localVariables.get(event);
        if (from == null) {
            return null;
        }
        return from.copy();
    }

    public static void withLocalVariables(Event provider, Event user, @NotNull Runnable action) {
        Variables.setLocalVariables(user, Variables.copyLocalVariables(provider));
        action.run();
        Variables.setLocalVariables(provider, Variables.copyLocalVariables(user));
        Variables.removeLocals(user);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public static Object getVariable(String name, @Nullable Event event, boolean local) {
        String n = caseInsensitiveVariables ? name.toLowerCase(Locale.ENGLISH) : name;
        if (local) {
            VariablesMap map = localVariables.get(event);
            if (map == null) {
                return null;
            }
            return map.getVariable(n);
        }
        try {
            VariableChange variableChange;
            variablesLock.readLock().lock();
            if (!changeQueue.isEmpty() && (variableChange = (VariableChange)changeQueue.stream().filter(change -> change.name.equals(n)).reduce((first, second) -> second).orElse(null)) != null) {
                Object object = variableChange.value;
                return object;
            }
            Object object = variables.getVariable(n);
            return object;
        }
        finally {
            variablesLock.readLock().unlock();
        }
    }

    public static Iterator<Pair<String, Object>> getVariableIterator(String name, final boolean local, final @Nullable Event event) {
        assert (name.endsWith("*"));
        Object val = Variables.getVariable(name, event, local);
        final String subName = StringUtils.substring(name, 0, -1);
        if (val == null) {
            return new EmptyIterator<Pair<String, Object>>();
        }
        assert (val instanceof TreeMap);
        final Iterator keys = new ArrayList(((Map)val).keySet()).iterator();
        return new Iterator<Pair<String, Object>>(){
            @Nullable
            private String key;
            @Nullable
            private Object next = null;

            @Override
            public boolean hasNext() {
                if (this.next != null) {
                    return true;
                }
                while (keys.hasNext()) {
                    this.key = (String)keys.next();
                    if (this.key == null) continue;
                    this.next = Variable.convertIfOldPlayer(subName + this.key, local, event, Variables.getVariable(subName + this.key, event, local));
                    if (this.next == null || this.next instanceof TreeMap) continue;
                    return true;
                }
                this.next = null;
                return false;
            }

            @Override
            public Pair<String, Object> next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                Pair<String, Object> n = new Pair<String, Object>(this.key, this.next);
                this.next = null;
                return n;
            }

            @Override
            public void remove() {
                if (this.key == null) {
                    throw new IllegalStateException();
                }
                Variables.deleteVariable(this.key, event, local);
            }
        };
    }

    public static void deleteVariable(String name, @Nullable Event event, boolean local) {
        Variables.setVariable(name, null, event, local);
    }

    public static void setVariable(String name, @Nullable Object value, @Nullable Event event, boolean local) {
        if (caseInsensitiveVariables) {
            name = name.toLowerCase(Locale.ENGLISH);
        }
        if (value != null) {
            assert (!name.endsWith("::*"));
            ClassInfo<?> ci = Classes.getSuperClassInfo(value.getClass());
            Class<?> sas = ci.getSerializeAs();
            if (sas != null) {
                value = Converters.convert(value, sas);
                assert (value != null) : String.valueOf(ci) + ", " + String.valueOf(sas);
            }
        }
        if (local) {
            assert (event != null) : name;
            VariablesMap map = localVariables.computeIfAbsent(event, e -> new VariablesMap());
            map.setVariable(name, value);
        } else {
            Variables.setVariable(name, value);
        }
    }

    static void setVariable(String name, @Nullable Object value) {
        if (variablesLock.writeLock().tryLock()) {
            try {
                if (!changeQueue.isEmpty()) {
                    Variables.processChangeQueue();
                }
                variables.setVariable(name, value);
                Variables.saveVariableChange(name, value);
            }
            finally {
                variablesLock.writeLock().unlock();
            }
        } else {
            Variables.queueVariableChange(name, value);
        }
    }

    private static void queueVariableChange(String name, @Nullable Object value) {
        changeQueue.add(new VariableChange(name, value));
    }

    static void processChangeQueue() {
        VariableChange change;
        while ((change = changeQueue.poll()) != null) {
            variables.setVariable(change.name, change.value);
            Variables.saveVariableChange(change.name, change.value);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean variableLoaded(String name, @Nullable Object value, VariablesStorage source) {
        assert (Bukkit.isPrimaryThread());
        if (value == null) {
            return false;
        }
        SynchronizedReference<Map<String, NonNullPair<Object, VariablesStorage>>> synchronizedReference = TEMP_VARIABLES;
        synchronized (synchronizedReference) {
            Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
            if (tvs != null) {
                VariablesStorage existingVariableStorage;
                NonNullPair<Object, VariablesStorage> existingVariable = tvs.get(name);
                if (existingVariable != null && (existingVariableStorage = existingVariable.getSecond()) != source) {
                    if (++loadConflicts <= 50) {
                        Skript.warning("The variable {" + name + "} was loaded twice from different databases (" + existingVariableStorage.getUserConfigurationName() + " and " + source.getUserConfigurationName() + "), only the one from " + source.getUserConfigurationName() + " will be kept.");
                    } else if (loadConflicts == 51) {
                        Skript.warning("[!] More than 50 variables were loaded more than once from different databases, no more warnings will be printed.");
                    }
                    existingVariableStorage.save(name, null, null);
                }
                tvs.put(name, new NonNullPair<Object, VariablesStorage>(value, source));
                return false;
            }
        }
        variablesLock.writeLock().lock();
        try {
            variables.setVariable(name, value);
        }
        finally {
            variablesLock.writeLock().unlock();
        }
        try {
            for (VariablesStorage variablesStorage : STORAGES) {
                if (!variablesStorage.accept(name)) continue;
                if (variablesStorage != source) {
                    SerializedVariable.Value serializedValue = Variables.serialize(value);
                    if (serializedValue == null) {
                        variablesStorage.save(name, null, null);
                    } else {
                        variablesStorage.save(name, serializedValue.type, serializedValue.data);
                    }
                    if (value != null) {
                        source.save(name, null, null);
                    }
                }
                return true;
            }
        }
        catch (Exception e) {
            Skript.exception((Throwable)e, "Error saving variable named " + name);
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static int onStoragesLoaded() {
        if (loadConflicts > 50) {
            Skript.warning("A total of " + loadConflicts + " variables were loaded more than once from different databases");
        }
        Skript.debug("Databases loaded, setting variables...");
        SynchronizedReference<Map<String, NonNullPair<Object, VariablesStorage>>> synchronizedReference = TEMP_VARIABLES;
        synchronized (synchronizedReference) {
            int n;
            Map<String, NonNullPair<Object, VariablesStorage>> tvs = TEMP_VARIABLES.get();
            TEMP_VARIABLES.set(null);
            assert (tvs != null);
            variablesLock.writeLock().lock();
            try {
                int unstoredVariables = 0;
                for (Map.Entry<String, NonNullPair<Object, VariablesStorage>> tv : tvs.entrySet()) {
                    if (Variables.variableLoaded(tv.getKey(), tv.getValue().getFirst(), tv.getValue().getSecond())) continue;
                    ++unstoredVariables;
                }
                for (VariablesStorage variablesStorage : STORAGES) {
                    variablesStorage.allLoaded();
                }
                Skript.debug("Variables set. Queue size = " + saveQueue.size());
                n = unstoredVariables;
                variablesLock.writeLock().unlock();
            }
            catch (Throwable throwable) {
                variablesLock.writeLock().unlock();
                throw throwable;
            }
            return n;
        }
    }

    public static SerializedVariable serialize(String name, @Nullable Object value) {
        SerializedVariable.Value var;
        assert (Bukkit.isPrimaryThread());
        try {
            var = Variables.serialize(value);
        }
        catch (Exception e) {
            throw Skript.exception((Throwable)e, "Error saving variable named " + name);
        }
        return new SerializedVariable(name, var);
    }

    public static @Nullable SerializedVariable.Value serialize(@Nullable Object value) {
        assert (Bukkit.isPrimaryThread());
        return Classes.serialize(value);
    }

    private static void saveVariableChange(String name, @Nullable Object value) {
        if (name.startsWith("-")) {
            return;
        }
        saveQueue.add(Variables.serialize(name, value));
    }

    public static void close() {
        try {
            variablesLock.writeLock().lock();
            Variables.processChangeQueue();
        }
        finally {
            variablesLock.writeLock().unlock();
        }
        while (saveQueue.size() > 0) {
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException interruptedException) {}
        }
        closed = true;
        saveThread.interrupt();
    }

    public static int numVariables() {
        try {
            variablesLock.readLock().lock();
            int n = Variables.variables.hashMap.size();
            return n;
        }
        finally {
            variablesLock.readLock().unlock();
        }
    }

    static {
        Variables.registerStorage(FlatFileStorage.class, "csv", "file", "flatfile");
        Variables.registerStorage(SQLiteStorage.class, "sqlite");
        Variables.registerStorage(MySQLStorage.class, "mysql");
        yggdrasil.registerSingleClass(Kleenean.class, "Kleenean");
        yggdrasil.registerClassResolver(new ConfigurationSerializer<ConfigurationSerializable>(){
            {
                this.info = Classes.getExactClassInfo(Object.class);
            }

            @Override
            @Nullable
            public String getID(@NotNull Class<?> c) {
                if (ConfigurationSerializable.class.isAssignableFrom(c) && Classes.getSuperClassInfo(c) == Classes.getExactClassInfo(Object.class)) {
                    return Variables.CONFIGURATION_SERIALIZABLE_PREFIX + ConfigurationSerialization.getAlias(c.asSubclass(ConfigurationSerializable.class));
                }
                return null;
            }

            @Override
            @Nullable
            public Class<? extends ConfigurationSerializable> getClass(@NotNull String id) {
                if (id.startsWith(Variables.CONFIGURATION_SERIALIZABLE_PREFIX)) {
                    return ConfigurationSerialization.getClassByAlias((String)id.substring(Variables.CONFIGURATION_SERIALIZABLE_PREFIX.length()));
                }
                return null;
            }
        });
        STORAGES = new ArrayList<VariablesStorage>();
        variablesLock = new ReentrantReadWriteLock(true);
        variables = new VariablesMap();
        localVariables = new ConcurrentHashMap<Event, VariablesMap>();
        changeQueue = new ConcurrentLinkedQueue<VariableChange>();
        TEMP_VARIABLES = new SynchronizedReference(new HashMap());
        loadConflicts = 0;
        saveQueue = new LinkedBlockingQueue<SerializedVariable>();
        closed = false;
        saveThread = Skript.newThread(() -> {
            while (!closed) {
                try {
                    SerializedVariable variable = saveQueue.take();
                    for (VariablesStorage variablesStorage : STORAGES) {
                        if (!variablesStorage.accept(variable.name)) continue;
                        variablesStorage.save(variable);
                    }
                }
                catch (InterruptedException interruptedException) {}
            }
        }, "Skript variable save thread");
    }

    private static class VariableChange {
        public final String name;
        @Nullable
        public final Object value;

        public VariableChange(String name, @Nullable Object value) {
            this.name = name;
            this.value = value;
        }
    }
}

