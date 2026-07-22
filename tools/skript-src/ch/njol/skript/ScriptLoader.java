/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.events.bukkit.PreScriptLoadEvent;
import ch.njol.skript.lang.ExecutionIntent;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Statement;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.CountingLogHandler;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.structures.StructOptions;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.skript.util.ExceptionUtils;
import ch.njol.skript.util.Task;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.variables.HintManager;
import ch.njol.util.OpenCloseable;
import ch.njol.util.StringUtils;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.bukkit.text.TextComponentParser;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.script.ScriptWarning;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.util.event.EventRegistry;

public class ScriptLoader {
    public static final String DISABLED_SCRIPT_PREFIX = "-";
    public static final int DISABLED_SCRIPT_PREFIX_LENGTH = "-".length();
    private static final Set<Script> loadedScripts = Collections.synchronizedSortedSet(new TreeSet<Script>(new Comparator<Script>(){

        @Override
        public int compare(Script s1, Script s2) {
            File f2Parent;
            File f1 = s1.getConfig().getFile();
            File f2 = s2.getConfig().getFile();
            if (f1 == null || f2 == null) {
                throw new IllegalArgumentException("Scripts will null config files cannot be sorted.");
            }
            File f1Parent = f1.getParentFile();
            if (this.isSubDir(f1Parent, f2Parent = f2.getParentFile())) {
                return -1;
            }
            if (this.isSubDir(f2Parent, f1Parent)) {
                return 1;
            }
            return f1.compareTo(f2);
        }

        private boolean isSubDir(File directory, File subDir) {
            for (File parentDir = directory.getParentFile(); parentDir != null; parentDir = parentDir.getParentFile()) {
                if (!subDir.equals(parentDir)) continue;
                return true;
            }
            return false;
        }
    }));
    private static final FileFilter loadedScriptFilter = f -> f != null && (f.isDirectory() && !f.getName().startsWith(".") || !f.isDirectory() && StringUtils.endsWithIgnoreCase(f.getName(), ".sk")) && !f.getName().startsWith(DISABLED_SCRIPT_PREFIX) && !f.isHidden();
    private static final Set<File> disabledScripts = Collections.synchronizedSet(new HashSet());
    private static final FileFilter disabledScriptFilter = f -> f != null && (f.isDirectory() && !f.getName().startsWith(".") || !f.isDirectory() && StringUtils.endsWithIgnoreCase(f.getName(), ".sk")) && f.getName().startsWith(DISABLED_SCRIPT_PREFIX) && !f.isHidden();
    private static final BlockingQueue<Runnable> loadQueue = new LinkedBlockingQueue<Runnable>();
    private static final ThreadGroup asyncLoaderThreadGroup = new ThreadGroup("Skript async loaders");
    private static final List<AsyncLoaderThread> loaderThreads = new ArrayList<AsyncLoaderThread>();
    private static int asyncLoaderSize;
    private static Executor executor;
    private static final EventRegistry<LoaderEvent> eventRegistry;

    private static ParserInstance getParser() {
        return ParserInstance.get();
    }

    @Nullable
    public static Script getScript(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("Something other than a file was provided.");
        }
        for (Script script : loadedScripts) {
            if (!file.equals(script.getConfig().getFile())) continue;
            return script;
        }
        return null;
    }

    public static Set<Script> getScripts(File directory) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Something other than a directory was provided.");
        }
        HashSet<Script> scripts = new HashSet<Script>();
        for (File file : directory.listFiles(loadedScriptFilter)) {
            if (file.isDirectory()) {
                scripts.addAll(ScriptLoader.getScripts(file));
                continue;
            }
            Script script = ScriptLoader.getScript(file);
            if (script == null) continue;
            scripts.add(script);
        }
        return scripts;
    }

    static void updateDisabledScripts(Path path) {
        disabledScripts.clear();
        try (Stream<Path> files = Files.walk(path, new FileVisitOption[0]);){
            files.map(Path::toFile).filter(disabledScriptFilter::accept).forEach(disabledScripts::add);
        }
        catch (Exception e) {
            Skript.exception((Throwable)e, "An error occurred while trying to update the list of disabled scripts!");
        }
    }

    public static boolean isAsync() {
        return asyncLoaderSize > 0;
    }

    public static boolean isParallel() {
        return asyncLoaderSize > 1;
    }

    public static @UnknownNullability Executor getExecutor() {
        return executor;
    }

    public static void setAsyncLoaderSize(int size) throws IllegalStateException {
        asyncLoaderSize = size;
        if (size <= 0) {
            for (AsyncLoaderThread thread : loaderThreads) {
                thread.cancelExecution();
            }
            return;
        }
        while (loaderThreads.size() > size) {
            AsyncLoaderThread thread = loaderThreads.remove(loaderThreads.size() - 1);
            thread.cancelExecution();
        }
        while (loaderThreads.size() < size) {
            loaderThreads.add(AsyncLoaderThread.create());
        }
        if (loaderThreads.size() != size) {
            throw new IllegalStateException();
        }
        executor = Executors.newFixedThreadPool(asyncLoaderSize, new ThreadFactory(){
            private final AtomicInteger threadId = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(asyncLoaderThreadGroup, runnable, "Skript async loaders thread " + this.threadId.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    private static <T> CompletableFuture<T> makeFuture(Supplier<T> supplier, OpenCloseable openCloseable) {
        CompletableFuture future = new CompletableFuture();
        Runnable task = () -> {
            try {
                Object t;
                openCloseable.open();
                try {
                    t = supplier.get();
                }
                finally {
                    openCloseable.close();
                }
                future.complete(t);
            }
            catch (Throwable t) {
                future.completeExceptionally(t);
                Skript.exception(t, new String[0]);
            }
        };
        if (ScriptLoader.isAsync() && Bukkit.isPrimaryThread()) {
            loadQueue.add(task);
        } else {
            task.run();
            assert (future.isDone());
        }
        return future;
    }

    public static CompletableFuture<ScriptInfo> loadScripts(File file, OpenCloseable openCloseable) {
        return ScriptLoader.loadScripts(ScriptLoader.loadStructures(file), openCloseable);
    }

    public static CompletableFuture<ScriptInfo> loadScripts(Set<File> files, OpenCloseable openCloseable) {
        return ScriptLoader.loadScripts(files.stream().sorted().map(ScriptLoader::loadStructures).flatMap(Collection::stream).collect(Collectors.toList()), openCloseable);
    }

    private static CompletableFuture<ScriptInfo> loadScripts(List<Config> configs, OpenCloseable openCloseable) {
        if (configs.isEmpty()) {
            return CompletableFuture.completedFuture(new ScriptInfo());
        }
        ScriptLoader.eventRegistry().events(ScriptPreInitEvent.class).forEach(event -> event.onPreInit(configs));
        Bukkit.getPluginManager().callEvent((Event)new PreScriptLoadEvent(configs));
        ScriptInfo scriptInfo = new ScriptInfo();
        ArrayList scripts = new ArrayList();
        ArrayList<CompletableFuture<Void>> scriptInfoFutures = new ArrayList<CompletableFuture<Void>>();
        for (Config config : configs) {
            if (config == null) {
                throw new NullPointerException();
            }
            CompletableFuture<Void> future = ScriptLoader.makeFuture(() -> {
                LoadingScriptInfo info = ScriptLoader.loadScript(config);
                scripts.add(info);
                scriptInfo.add(new ScriptInfo(1, info.structures.size()));
                return null;
            }, openCloseable);
            scriptInfoFutures.add(future);
        }
        return ((CompletableFuture)CompletableFuture.allOf(scriptInfoFutures.toArray(new CompletableFuture[0])).thenApply(unused -> {
            ParserInstance parser = ScriptLoader.getParser();
            try {
                openCloseable.open();
                List loadingStructures = scripts.stream().flatMap(info -> info.structures.stream().map(structure -> {
                    record LoadingStructure(LoadingScriptInfo loadingScriptInfo, Structure structure) {
                    }
                    return new LoadingStructure((LoadingScriptInfo)info, (Structure)structure);
                })).sorted(Comparator.comparing(pair -> pair.structure().getPriority())).collect(Collectors.toCollection(ArrayList::new));
                loadingStructures.removeIf(loadingStructure -> {
                    LoadingScriptInfo loadingInfo = loadingStructure.loadingScriptInfo();
                    Structure structure = loadingStructure.structure();
                    parser.setActive(loadingInfo.script);
                    parser.setCurrentStructure(structure);
                    parser.setNode(loadingInfo.nodeMap.get(structure));
                    try {
                        if (!structure.preLoad()) {
                            loadingInfo.structures.remove(structure);
                            return true;
                        }
                    }
                    catch (Exception e) {
                        Skript.exception((Throwable)e, "An error occurred while trying to preLoad a Structure.");
                        loadingInfo.structures.remove(structure);
                        return true;
                    }
                    return false;
                });
                parser.setInactive();
                loadingStructures.removeIf(loadingStructure -> {
                    LoadingScriptInfo loadingInfo = loadingStructure.loadingScriptInfo();
                    Structure structure = loadingStructure.structure();
                    parser.setActive(loadingInfo.script);
                    parser.setCurrentStructure(structure);
                    parser.setNode(loadingInfo.nodeMap.get(structure));
                    try {
                        if (!structure.load()) {
                            loadingInfo.structures.remove(structure);
                            return true;
                        }
                    }
                    catch (Exception e) {
                        Skript.exception((Throwable)e, "An error occurred while trying to load a Structure.");
                        loadingInfo.structures.remove(structure);
                        return true;
                    }
                    return false;
                });
                parser.setInactive();
                loadingStructures.removeIf(loadingStructure -> {
                    LoadingScriptInfo loadingInfo = loadingStructure.loadingScriptInfo();
                    Structure structure = loadingStructure.structure();
                    parser.setActive(loadingInfo.script);
                    parser.setCurrentStructure(structure);
                    parser.setNode(loadingInfo.nodeMap.get(structure));
                    try {
                        if (!structure.postLoad()) {
                            loadingInfo.structures.remove(structure);
                            return true;
                        }
                    }
                    catch (Exception e) {
                        Skript.exception((Throwable)e, "An error occurred while trying to postLoad a Structure.");
                        loadingInfo.structures.remove(structure);
                        return true;
                    }
                    return false;
                });
                parser.setInactive();
                scripts.forEach(loadingInfo -> {
                    Script script = loadingInfo.script;
                    parser.setActive(script);
                    parser.setNode(script.getConfig().getMainNode());
                    ScriptLoader.eventRegistry().events(ScriptLoadEvent.class).forEach(event -> event.onLoad(parser, script));
                    script.eventRegistry().events(ScriptLoadEvent.class).forEach(event -> event.onLoad(parser, script));
                });
                parser.setInactive();
                ScriptInfo scriptInfo2 = scriptInfo;
                return scriptInfo2;
            }
            catch (Exception e) {
                throw Skript.exception((Throwable)e, new String[0]);
            }
            finally {
                parser.setInactive();
                openCloseable.close();
            }
        })).exceptionally(t -> {
            throw Skript.exception(t, new String[0]);
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static LoadingScriptInfo loadScript(Config config) {
        if (config.getFile() == null) {
            throw new IllegalArgumentException("A config must have a file to be loaded.");
        }
        ParserInstance parser = ScriptLoader.getParser();
        HashMap<Structure, Node> nodeMap = new HashMap<Structure, Node>();
        ArrayList<Structure> structures = new ArrayList<Structure>();
        Script script = new Script(config, structures);
        parser.setActive(script);
        try {
            if (SkriptConfig.keepConfigsLoaded.value().booleanValue()) {
                SkriptConfig.configs.add(config);
            }
            try (CountingLogHandler ignored = new CountingLogHandler(SkriptLogger.SEVERE).start();){
                for (Node node : config.getMainNode()) {
                    Structure structure;
                    if (!(node instanceof SimpleNode) && !(node instanceof SectionNode)) {
                        Skript.error("could not interpret line as a structure");
                        continue;
                    }
                    String line = node.getKey();
                    if (line == null || !SkriptParser.validateLine(line = ScriptLoader.replaceOptions(line))) continue;
                    if (Skript.logVeryHigh() && !Skript.debug()) {
                        Skript.info("loading trigger '" + line + "'");
                    }
                    if ((structure = Structure.parse(line, node, "Can't understand this structure: " + line)) == null) continue;
                    structures.add(structure);
                    nodeMap.put(structure, node);
                }
                if (Skript.logHigh()) {
                    int count;
                    Skript.info("loaded " + count + " structure" + ((count = structures.size()) == 1 ? "" : "s") + " from '" + config.getFileName() + "'");
                }
            }
        }
        catch (Exception e) {
            Skript.exception((Throwable)e, "Could not load " + config.getFileName());
        }
        finally {
            parser.setInactive();
        }
        Callable<Void> callable = () -> {
            File file = config.getFile();
            assert (file != null);
            File disabledFile = new File(file.getParentFile(), DISABLED_SCRIPT_PREFIX + file.getName());
            disabledScripts.remove(disabledFile);
            loadedScripts.add(script);
            ScriptLoader.eventRegistry().events(ScriptInitEvent.class).forEach(event -> event.onInit(script));
            return null;
        };
        if (ScriptLoader.isAsync()) {
            Task.callSync(callable);
        } else {
            try {
                callable.call();
            }
            catch (Exception e) {
                Skript.exception((Throwable)e, new String[0]);
            }
        }
        return new LoadingScriptInfo(script, structures, nodeMap);
    }

    private static List<Config> loadStructures(File directory) {
        if (!directory.isDirectory()) {
            Config config = ScriptLoader.loadStructure(directory);
            return config != null ? Collections.singletonList(config) : Collections.emptyList();
        }
        try {
            directory = directory.getCanonicalFile();
        }
        catch (IOException e) {
            Skript.exception((Throwable)e, "An exception occurred while trying to get the canonical file of: " + String.valueOf(directory));
            return new ArrayList<Config>();
        }
        Object[] files = directory.listFiles(loadedScriptFilter);
        assert (files != null);
        Arrays.sort(files);
        ArrayList<Config> loadedDirectories = new ArrayList<Config>(files.length);
        ArrayList<Config> loadedFiles = new ArrayList<Config>(files.length);
        for (Object file : files) {
            if (((File)file).isDirectory()) {
                loadedDirectories.addAll(ScriptLoader.loadStructures((File)file));
                continue;
            }
            Config cfg = ScriptLoader.loadStructure((File)file);
            if (cfg == null) continue;
            loadedFiles.add(cfg);
        }
        loadedDirectories.addAll(loadedFiles);
        return loadedDirectories;
    }

    @Nullable
    private static Config loadStructure(File file) {
        try {
            file = file.getCanonicalFile();
        }
        catch (IOException e) {
            Skript.exception((Throwable)e, "An exception occurred while trying to get the canonical file of: " + String.valueOf(file));
            return null;
        }
        if (!file.exists()) {
            Script script = ScriptLoader.getScript(file);
            if (script != null) {
                ScriptLoader.unloadScript(script);
            }
            return null;
        }
        try {
            String name = Skript.getInstance().getDataFolder().toPath().toAbsolutePath().resolve("scripts").relativize(file.toPath().toAbsolutePath()).toString();
            return ScriptLoader.loadStructure(Files.newInputStream(file.toPath(), new OpenOption[0]), name);
        }
        catch (IOException e) {
            Skript.error("Could not load " + file.getName() + ": " + ExceptionUtils.toString(e));
            return null;
        }
    }

    @Nullable
    private static Config loadStructure(InputStream source, String name) {
        try {
            return new Config(source, name, Skript.getInstance().getDataFolder().toPath().resolve("scripts").resolve(name).toFile().getCanonicalFile(), true, false, ":");
        }
        catch (IOException e) {
            Skript.error("Could not load " + name + ": " + ExceptionUtils.toString(e));
            return null;
        }
    }

    public static ScriptInfo unloadScripts(Set<Script> scripts) {
        for (Script script2 : scripts) {
            if (!loadedScripts.contains(script2)) {
                throw new SkriptAPIException("The script at '" + String.valueOf(script2.getConfig().getPath()) + "' is not loaded!");
            }
            if (script2.getConfig().getFile() != null) continue;
            throw new IllegalArgumentException("A script must have a file to be unloaded.");
        }
        ParserInstance parser = ScriptLoader.getParser();
        record UnloadingStructure(Script script, Structure structure) {
        }
        Comparator<UnloadingStructure> unloadComparator = Comparator.comparing(unloadingStructure -> unloadingStructure.structure().getPriority());
        unloadComparator = unloadComparator.reversed();
        List unloadingStructures = scripts.stream().flatMap(script -> script.getStructures().stream().map(structure -> new UnloadingStructure((Script)script, (Structure)structure))).sorted(unloadComparator).collect(Collectors.toCollection(ArrayList::new));
        for (Script script3 : scripts) {
            ScriptLoader.eventRegistry().events(ScriptUnloadEvent.class).forEach(event -> event.onUnload(parser, script3));
            script3.eventRegistry().events(ScriptUnloadEvent.class).forEach(event -> event.onUnload(parser, script3));
        }
        for (UnloadingStructure unloadingStructure2 : unloadingStructures) {
            Script script4 = unloadingStructure2.script();
            Structure structure = unloadingStructure2.structure();
            parser.setActive(script4);
            structure.unload();
        }
        parser.setInactive();
        ScriptInfo info = new ScriptInfo();
        for (UnloadingStructure unloadingStructure3 : unloadingStructures) {
            Script script5 = unloadingStructure3.script();
            Structure structure = unloadingStructure3.structure();
            ++info.structures;
            parser.setActive(script5);
            structure.postUnload();
        }
        parser.setInactive();
        for (Script script4 : scripts) {
            ++info.files;
            script4.clearData();
            script4.invalidate();
            loadedScripts.remove(script4);
            File scriptFile = script4.getConfig().getFile();
            assert (scriptFile != null);
            disabledScripts.add(new File(scriptFile.getParentFile(), DISABLED_SCRIPT_PREFIX + scriptFile.getName()));
        }
        return info;
    }

    public static ScriptInfo unloadScript(Script script) {
        return ScriptLoader.unloadScripts(Collections.singleton(script));
    }

    public static CompletableFuture<ScriptInfo> reloadScript(Script script, OpenCloseable openCloseable) {
        return ScriptLoader.reloadScripts(Collections.singleton(script), openCloseable);
    }

    public static CompletableFuture<ScriptInfo> reloadScripts(Set<Script> scripts, OpenCloseable openCloseable) {
        ScriptLoader.unloadScripts(scripts);
        ArrayList<Config> configs = new ArrayList<Config>();
        for (Script script : scripts) {
            Config config = ScriptLoader.loadStructure(script.getConfig().getFile());
            if (config == null) {
                return CompletableFuture.completedFuture(new ScriptInfo());
            }
            configs.add(config);
        }
        return ScriptLoader.loadScripts(configs, openCloseable);
    }

    public static String replaceOptions(String string) {
        ParserInstance parser = ScriptLoader.getParser();
        if (!parser.isActive()) {
            return string;
        }
        StructOptions.OptionsData optionsData = parser.getCurrentScript().getData(StructOptions.OptionsData.class);
        if (optionsData == null) {
            return string;
        }
        return optionsData.replaceOptions(string);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static ArrayList<TriggerItem> loadItems(SectionNode node) {
        ParserInstance parser = ScriptLoader.getParser();
        if (Skript.debug()) {
            parser.setIndentation(parser.getIndentation() + "    ");
        }
        ArrayList<TriggerItem> items = new ArrayList<TriggerItem>();
        parser.getHintManager().enterScope(true);
        boolean freezeScope = false;
        boolean executionStops = false;
        for (Node subNode : node) {
            parser.setNode(subNode);
            String subNodeKey = subNode.getKey();
            if (subNodeKey == null) {
                throw new IllegalArgumentException("Encountered node with null key: '" + String.valueOf(subNode) + "'");
            }
            String expr = ScriptLoader.replaceOptions(subNodeKey);
            if (!SkriptParser.validateLine(expr)) continue;
            TriggerItem item = null;
            if (subNode instanceof SimpleNode) {
                long timeTaken;
                long start = System.currentTimeMillis();
                item = Statement.parse(expr, items, "Can't understand this condition/effect: " + expr);
                if (item == null) continue;
                long requiredTime = SkriptConfig.longParseTimeWarningThreshold.value().getAs(Timespan.TimePeriod.MILLISECOND);
                if (requiredTime > 0L && (timeTaken = System.currentTimeMillis() - start) > requiredTime) {
                    Skript.warning("The current line took a long time to parse (" + String.valueOf(new Timespan(timeTaken)) + "). Avoid using long lines and use parentheses to create clearer instructions.");
                }
                if (Skript.debug() || subNode.debug()) {
                    Skript.debug(TextComponentParser.instance().escape(parser.getIndentation() + item.toString(null, true)));
                }
                items.add(item);
            } else {
                block25: {
                    if (!(subNode instanceof SectionNode)) continue;
                    SectionNode subSection = (SectionNode)subNode;
                    RetainingLogHandler handler = SkriptLogger.startRetainingLog();
                    try {
                        parser.getHintManager().enterScope(false);
                        item = Section.parse(expr, "Can't understand this section: " + expr, subSection, items);
                        if (item != null) break block25;
                        RetainingLogHandler backup = handler.backup();
                        handler.clear();
                        item = Statement.parse(expr, "Can't understand this condition/effect: " + expr, subSection, items);
                        if (item == null) {
                            String backupError;
                            if (!handler.hasErrors()) {
                                handler.restore(backup);
                                continue;
                            }
                            LogEntry errorEntry = handler.getFirstError();
                            assert (errorEntry != null);
                            String error = errorEntry.getMessage();
                            if (error.contains("Can't understand this condition/effect:")) {
                                handler.restore(backup);
                                continue;
                            }
                            LogEntry backupErrorEntry = backup.getFirstError();
                            if (backupErrorEntry == null || !(backupError = backupErrorEntry.getMessage()).contains("tried to claim the current section, but it was already claimed by") && (backupError.contains("Can't understand this section: ") || !error.contains("is a valid statement but cannot function as a section (:) because there is no syntax in the line to manage it."))) continue;
                            handler.restore(backup);
                            continue;
                        }
                    }
                    finally {
                        HintManager hintManager = parser.getHintManager();
                        if (item == null) {
                            hintManager.clearScope(0, false);
                        }
                        hintManager.exitScope();
                        RetainingLogHandler afterParse = handler.backup();
                        handler.clear();
                        handler.printLog();
                        if (item != null && (Skript.debug() || subNode.debug())) {
                            Skript.debug(TextComponentParser.instance().escape(parser.getIndentation() + item.toString(null, true)));
                        }
                        afterParse.printLog();
                        continue;
                    }
                }
                items.add(item);
            }
            if (executionStops && !SkriptConfig.disableUnreachableCodeWarnings.value().booleanValue() && parser.isActive() && !parser.getCurrentScript().suppressesWarning(ScriptWarning.UNREACHABLE_CODE)) {
                Skript.warning("Unreachable code. The previous statement stops further execution.");
            }
            if (!(executionStops = item.executionIntent() != null) || freezeScope) continue;
            freezeScope = true;
            ExecutionIntent executionIntent = item.executionIntent();
            if (!(executionIntent instanceof ExecutionIntent.StopSections)) continue;
            ExecutionIntent.StopSections intent = (ExecutionIntent.StopSections)executionIntent;
            parser.getHintManager().mergeScope(0, intent.levels(), true);
        }
        if (freezeScope) {
            parser.getHintManager().clearScope(0, false);
        }
        parser.getHintManager().exitScope();
        for (int i = 0; i < items.size() - 1; ++i) {
            ((TriggerItem)items.get(i)).setNext(items.get(i + 1));
        }
        parser.setNode(node);
        if (Skript.debug()) {
            parser.setIndentation(parser.getIndentation().substring(0, parser.getIndentation().length() - 4));
        }
        return items;
    }

    @ApiStatus.Internal
    public static Script createDummyScript(String name, @Nullable File file) {
        Config config = new Config(name, file);
        return new Script(config, Collections.emptyList());
    }

    public static Set<Script> getLoadedScripts() {
        return Collections.unmodifiableSet(new HashSet<Script>(loadedScripts));
    }

    public static Set<File> getDisabledScripts() {
        return Collections.unmodifiableSet(new HashSet<File>(disabledScripts));
    }

    public static FileFilter getLoadedScriptsFilter() {
        return loadedScriptFilter;
    }

    public static FileFilter getDisabledScriptsFilter() {
        return disabledScriptFilter;
    }

    public static EventRegistry<LoaderEvent> eventRegistry() {
        return eventRegistry;
    }

    @Nullable
    public static File getScriptFromName(String script) {
        return ScriptLoader.getScriptFromName(script, Skript.getInstance().getScriptsFolder());
    }

    @Nullable
    public static File getScriptFromName(String script, File directory) {
        File scriptFile;
        if (((String)script).endsWith("/") || ((String)script).endsWith("\\")) {
            script = ((String)script).replace('/', File.separatorChar).replace('\\', File.separatorChar);
        } else if (!StringUtils.endsWithIgnoreCase((String)script, ".sk")) {
            int dot = ((String)script).lastIndexOf(46);
            if (dot > 0 && !((String)script).substring(dot + 1).equals("")) {
                return null;
            }
            script = (String)script + ".sk";
        }
        if (((String)script).startsWith(DISABLED_SCRIPT_PREFIX)) {
            script = ((String)script).substring(DISABLED_SCRIPT_PREFIX_LENGTH);
        }
        if (!(scriptFile = new File(directory, (String)script)).exists() && !(scriptFile = new File(scriptFile.getParentFile(), DISABLED_SCRIPT_PREFIX + scriptFile.getName())).exists()) {
            return null;
        }
        try {
            if (TestMode.ENABLED || scriptFile.getCanonicalPath().startsWith(directory.getCanonicalPath() + File.separator)) {
                return scriptFile.getCanonicalFile();
            }
            return null;
        }
        catch (IOException e) {
            throw Skript.exception((Throwable)e, "An exception occurred while trying to get the script file from the string '" + (String)script + "'");
        }
    }

    static {
        eventRegistry = new EventRegistry();
    }

    private static class AsyncLoaderThread
    extends Thread {
        private boolean shouldRun = true;

        public static AsyncLoaderThread create() {
            AsyncLoaderThread thread = new AsyncLoaderThread();
            thread.start();
            return thread;
        }

        private AsyncLoaderThread() {
            super(asyncLoaderThreadGroup, (Runnable)null);
        }

        @Override
        public void run() {
            while (this.shouldRun) {
                try {
                    Runnable runnable = loadQueue.poll(100L, TimeUnit.MILLISECONDS);
                    if (runnable == null) continue;
                    runnable.run();
                }
                catch (InterruptedException e) {
                    Skript.exception((Throwable)e, new String[0]);
                }
            }
        }

        public void cancelExecution() {
            this.shouldRun = false;
        }
    }

    public static class ScriptInfo {
        public int files;
        public int structures;

        public ScriptInfo() {
        }

        public ScriptInfo(int numFiles, int numStructures) {
            this.files = numFiles;
            this.structures = numStructures;
        }

        public ScriptInfo(ScriptInfo other) {
            this.files = other.files;
            this.structures = other.structures;
        }

        public void add(ScriptInfo other) {
            this.files += other.files;
            this.structures += other.structures;
        }

        public void subtract(ScriptInfo other) {
            this.files -= other.files;
            this.structures -= other.structures;
        }

        public String toString() {
            return "ScriptInfo{files=" + this.files + ",structures=" + this.structures + "}";
        }
    }

    @FunctionalInterface
    public static interface ScriptPreInitEvent
    extends LoaderEvent {
        public void onPreInit(Collection<Config> var1);
    }

    private static class LoadingScriptInfo {
        public final Script script;
        public final List<Structure> structures;
        public final Map<Structure, Node> nodeMap;

        public LoadingScriptInfo(Script script, List<Structure> structures, Map<Structure, Node> nodeMap) {
            this.script = script;
            this.structures = structures;
            this.nodeMap = nodeMap;
        }
    }

    @FunctionalInterface
    public static interface ScriptUnloadEvent
    extends LoaderEvent,
    Script.Event {
        public void onUnload(ParserInstance var1, Script var2);
    }

    @FunctionalInterface
    public static interface ScriptInitEvent
    extends LoaderEvent {
        public void onInit(Script var1);
    }

    @FunctionalInterface
    public static interface ScriptLoadEvent
    extends LoaderEvent,
    Script.Event {
        public void onLoad(ParserInstance var1, Script var2);
    }

    public static interface LoaderEvent
    extends org.skriptlang.skript.util.event.Event {
    }
}

