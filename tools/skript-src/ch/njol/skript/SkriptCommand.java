/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.GsonBuilder
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.PluginDescriptionFile
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.SkriptUpdater;
import ch.njol.skript.aliases.Aliases;
import ch.njol.skript.command.CommandHelp;
import ch.njol.skript.doc.Documentation;
import ch.njol.skript.doc.HTMLGenerator;
import ch.njol.skript.doc.JSONGenerator;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.PluralizingArgsMessage;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.RedirectingLogHandler;
import ch.njol.skript.log.TestingLogHandler;
import ch.njol.skript.log.TimingLogHandler;
import ch.njol.skript.test.runner.SkriptTestEvent;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.skript.test.runner.TestTracker;
import ch.njol.skript.test.utils.TestResults;
import ch.njol.skript.util.ExceptionUtils;
import ch.njol.skript.util.FileUtils;
import ch.njol.skript.util.SkriptColor;
import ch.njol.util.OpenCloseable;
import ch.njol.util.StringUtils;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.lang.script.Script;

public class SkriptCommand
implements CommandExecutor {
    private static final String CONFIG_NODE = "skript command";
    private static final ArgsMessage m_reloading = new ArgsMessage("skript command.reload.reloading");
    private static final CommandHelp SKRIPT_COMMAND_HELP = new CommandHelp("<gray>/<gold>skript", SkriptColor.LIGHT_CYAN, "skript command.help").add(new CommandHelp("reload", SkriptColor.DARK_RED).add("all").add("config").add("aliases").add("scripts").add("<script>")).add(new CommandHelp("enable", SkriptColor.DARK_RED).add("all").add("<script>")).add(new CommandHelp("disable", SkriptColor.DARK_RED).add("all").add("<script>")).add(new CommandHelp("update", SkriptColor.DARK_RED).add("check").add("changes")).add("list").add("show").add("info").add("help");
    private static final ArgsMessage m_reloaded;
    private static final ArgsMessage m_reload_error;
    private static final ArgsMessage m_invalid_script;
    private static final ArgsMessage m_invalid_folder;

    private static void reloading(CommandSender sender, String what, RedirectingLogHandler logHandler, Object ... args) {
        what = args.length == 0 ? Language.get("skript command.reload." + what) : Language.format("skript command.reload." + what, args);
        String message = StringUtils.fixCapitalization(m_reloading.toString(what));
        Skript.info(sender, message);
        String text = Language.format("skript command.reload.player reload", sender.getName(), what);
        logHandler.log(new LogEntry(Level.INFO, text), sender);
    }

    private static void reloaded(CommandSender sender, RedirectingLogHandler logHandler, TimingLogHandler timingLogHandler, String what, Object ... args) {
        what = args.length == 0 ? Language.get("skript command.reload." + what) : PluralizingArgsMessage.format(Language.format("skript command.reload." + what, args));
        String timeTaken = String.valueOf(timingLogHandler.getTimeTaken());
        if (logHandler.numErrors() == 0) {
            String message = StringUtils.fixCapitalization(PluralizingArgsMessage.format(m_reloaded.toString(what, timeTaken)));
            logHandler.log(new LogEntry(Level.INFO, message));
        } else {
            String message = StringUtils.fixCapitalization(PluralizingArgsMessage.format(m_reload_error.toString(what, logHandler.numErrors(), timeTaken)));
            logHandler.log(new LogEntry(Level.SEVERE, message));
        }
    }

    private static void info(CommandSender sender, String what, Object ... args) {
        what = args.length == 0 ? Language.get("skript command." + what) : PluralizingArgsMessage.format(Language.format("skript command." + what, args));
        Skript.info(sender, StringUtils.fixCapitalization(what));
    }

    private static void error(CommandSender sender, String what, Object ... args) {
        what = args.length == 0 ? Language.get("skript command." + what) : PluralizingArgsMessage.format(Language.format("skript command." + what, args));
        Skript.error(sender, StringUtils.fixCapitalization(what));
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!SKRIPT_COMMAND_HELP.test(sender, args)) {
            return true;
        }
        HashSet<CommandSender> recipients = new HashSet<CommandSender>();
        recipients.add(sender);
        if (args[0].equalsIgnoreCase("reload")) {
            recipients.addAll(Bukkit.getOnlinePlayers().stream().filter(player -> player.hasPermission("skript.reloadnotify")).collect(Collectors.toSet()));
        }
        try {
            Iterator<SkriptAddon> e7;
            block107: {
                File scriptFile;
                RedirectingLogHandler logHandler = new RedirectingLogHandler(recipients, "").start();
                TimingLogHandler timingLogHandler = new TimingLogHandler().start();
                if (args[0].equalsIgnoreCase("reload")) {
                    if (args[1].equalsIgnoreCase("all")) {
                        SkriptCommand.reloading(sender, "config, aliases and scripts", logHandler, new Object[0]);
                        SkriptConfig.load();
                        Aliases.clear();
                        Aliases.loadAsync().thenRun(() -> {
                            ScriptLoader.unloadScripts(ScriptLoader.getLoadedScripts());
                            ScriptLoader.loadScripts(Skript.getInstance().getScriptsFolder(), OpenCloseable.combine(logHandler, timingLogHandler)).thenAccept(info -> {
                                if (info.files == 0) {
                                    Skript.warning(Skript.m_no_scripts.toString());
                                }
                                SkriptCommand.reloaded(sender, logHandler, timingLogHandler, "config, aliases and scripts", new Object[0]);
                            });
                        });
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("scripts")) {
                        SkriptCommand.reloading(sender, "scripts", logHandler, new Object[0]);
                        ScriptLoader.unloadScripts(ScriptLoader.getLoadedScripts());
                        ScriptLoader.loadScripts(Skript.getInstance().getScriptsFolder(), OpenCloseable.combine(logHandler, timingLogHandler)).thenAccept(info -> {
                            if (info.files == 0) {
                                Skript.warning(Skript.m_no_scripts.toString());
                            }
                            SkriptCommand.reloaded(sender, logHandler, timingLogHandler, "scripts", new Object[0]);
                        });
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("config")) {
                        SkriptCommand.reloading(sender, "main config", logHandler, new Object[0]);
                        SkriptConfig.load();
                        SkriptCommand.reloaded(sender, logHandler, timingLogHandler, "main config", new Object[0]);
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("aliases")) {
                        SkriptCommand.reloading(sender, "aliases", logHandler, new Object[0]);
                        Aliases.clear();
                        Aliases.loadAsync().thenRun(() -> SkriptCommand.reloaded(sender, logHandler, timingLogHandler, "aliases", new Object[0]));
                        return true;
                    }
                    scriptFile = SkriptCommand.getScriptFromArgs(sender, args);
                    if (scriptFile == null) {
                        boolean bl = true;
                        return bl;
                    }
                    if (scriptFile.isDirectory()) {
                        String fileName = scriptFile.getName();
                        SkriptCommand.reloading(sender, "scripts in folder", logHandler, fileName);
                        ScriptLoader.unloadScripts(ScriptLoader.getScripts(scriptFile));
                        ScriptLoader.loadScripts(scriptFile, OpenCloseable.combine(logHandler, timingLogHandler)).thenAccept(scriptInfo -> {
                            if (scriptInfo.files == 0) {
                                SkriptCommand.info(sender, "reload.empty folder", fileName);
                            } else if (logHandler.numErrors() == 0) {
                                SkriptCommand.reloaded(sender, logHandler, timingLogHandler, "x scripts in folder success", fileName, scriptInfo.files);
                            } else {
                                SkriptCommand.reloaded(sender, logHandler, timingLogHandler, "x scripts in folder error", fileName, scriptInfo.files);
                            }
                        });
                        return true;
                    }
                    if (ScriptLoader.getDisabledScriptsFilter().accept(scriptFile)) {
                        SkriptCommand.info(sender, "reload.script disabled", scriptFile.getName().substring(ScriptLoader.DISABLED_SCRIPT_PREFIX_LENGTH), StringUtils.join(args, " ", 1, args.length));
                        boolean bl = true;
                        return bl;
                    }
                    SkriptCommand.reloading(sender, "script", logHandler, scriptFile.getName());
                    Script script2 = ScriptLoader.getScript(scriptFile);
                    if (script2 != null) {
                        ScriptLoader.unloadScript(script2);
                    }
                    ScriptLoader.loadScripts(scriptFile, OpenCloseable.combine(logHandler, timingLogHandler)).thenAccept(scriptInfo -> SkriptCommand.reloaded(sender, logHandler, timingLogHandler, "script", scriptFile.getName()));
                    return true;
                }
                if (args[0].equalsIgnoreCase("enable")) {
                    Set<File> scriptFiles;
                    if (args[1].equalsIgnoreCase("all")) {
                        try {
                            SkriptCommand.info(sender, "enable.all.enabling", new Object[0]);
                            ScriptLoader.loadScripts(SkriptCommand.toggleFiles(Skript.getInstance().getScriptsFolder(), true), (OpenCloseable)logHandler).thenAccept(scriptInfo -> {
                                if (logHandler.numErrors() == 0) {
                                    SkriptCommand.info(sender, "enable.all.enabled", new Object[0]);
                                } else {
                                    SkriptCommand.error(sender, "enable.all.error", logHandler.numErrors());
                                }
                            });
                            return true;
                        }
                        catch (IOException e2) {
                            SkriptCommand.error(sender, "enable.all.io error", ExceptionUtils.toString(e2));
                            return true;
                        }
                    }
                    scriptFile = SkriptCommand.getScriptFromArgs(sender, args);
                    if (scriptFile == null) {
                        boolean fileName = true;
                        return fileName;
                    }
                    if (!scriptFile.isDirectory()) {
                        if (ScriptLoader.getLoadedScriptsFilter().accept(scriptFile)) {
                            SkriptCommand.info(sender, "enable.single.already enabled", scriptFile.getName(), StringUtils.join(args, " ", 1, args.length));
                            boolean fileName = true;
                            return fileName;
                        }
                        try {
                            scriptFile = SkriptCommand.toggleFile(scriptFile, true);
                        }
                        catch (IOException e3) {
                            SkriptCommand.error(sender, "enable.single.io error", scriptFile.getName().substring(ScriptLoader.DISABLED_SCRIPT_PREFIX_LENGTH), ExceptionUtils.toString(e3));
                            boolean bl = true;
                            if (timingLogHandler != null) {
                                timingLogHandler.close();
                            }
                            if (logHandler == null) return bl;
                            logHandler.close();
                            return bl;
                        }
                        String fileName = scriptFile.getName();
                        SkriptCommand.info(sender, "enable.single.enabling", fileName);
                        ScriptLoader.loadScripts(scriptFile, (OpenCloseable)logHandler).thenAccept(scriptInfo -> {
                            if (logHandler.numErrors() == 0) {
                                SkriptCommand.info(sender, "enable.single.enabled", fileName);
                            } else {
                                SkriptCommand.error(sender, "enable.single.error", fileName, logHandler.numErrors());
                            }
                        });
                        return true;
                    }
                    try {
                        scriptFiles = SkriptCommand.toggleFiles(scriptFile, true);
                    }
                    catch (IOException e4) {
                        SkriptCommand.error(sender, "enable.folder.io error", scriptFile.getName(), ExceptionUtils.toString(e4));
                        boolean bl = true;
                        if (timingLogHandler != null) {
                            timingLogHandler.close();
                        }
                        if (logHandler == null) return bl;
                        logHandler.close();
                        return bl;
                    }
                    if (scriptFiles.isEmpty()) {
                        SkriptCommand.info(sender, "enable.folder.empty", scriptFile.getName());
                        boolean e4 = true;
                        return e4;
                    }
                    String fileName = scriptFile.getName();
                    SkriptCommand.info(sender, "enable.folder.enabling", fileName, scriptFiles.size());
                    ScriptLoader.loadScripts(scriptFiles, (OpenCloseable)logHandler).thenAccept(scriptInfo -> {
                        if (logHandler.numErrors() == 0) {
                            SkriptCommand.info(sender, "enable.folder.enabled", fileName, scriptInfo.files);
                        } else {
                            SkriptCommand.error(sender, "enable.folder.error", fileName, logHandler.numErrors());
                        }
                    });
                    return true;
                }
                if (args[0].equalsIgnoreCase("disable")) {
                    Set<File> scripts;
                    if (args[1].equalsIgnoreCase("all")) {
                        ScriptLoader.unloadScripts(ScriptLoader.getLoadedScripts());
                        try {
                            SkriptCommand.toggleFiles(Skript.getInstance().getScriptsFolder(), false);
                            SkriptCommand.info(sender, "disable.all.disabled", new Object[0]);
                            return true;
                        }
                        catch (IOException e5) {
                            SkriptCommand.error(sender, "disable.all.io error", ExceptionUtils.toString(e5));
                            return true;
                        }
                    }
                    scriptFile = SkriptCommand.getScriptFromArgs(sender, args);
                    if (scriptFile == null) {
                        boolean scriptFiles = true;
                        return scriptFiles;
                    }
                    if (!scriptFile.isDirectory()) {
                        if (ScriptLoader.getDisabledScriptsFilter().accept(scriptFile)) {
                            SkriptCommand.info(sender, "disable.single.already disabled", scriptFile.getName().substring(ScriptLoader.DISABLED_SCRIPT_PREFIX_LENGTH));
                            boolean scriptFiles = true;
                            return scriptFiles;
                        }
                        Script script3 = ScriptLoader.getScript(scriptFile);
                        if (script3 != null) {
                            ScriptLoader.unloadScript(script3);
                        }
                        String fileName = scriptFile.getName();
                        try {
                            SkriptCommand.toggleFile(scriptFile, false);
                        }
                        catch (IOException e6) {
                            SkriptCommand.error(sender, "disable.single.io error", scriptFile.getName(), ExceptionUtils.toString(e6));
                            boolean bl = true;
                            if (timingLogHandler != null) {
                                timingLogHandler.close();
                            }
                            if (logHandler == null) return bl;
                            logHandler.close();
                            return bl;
                        }
                        SkriptCommand.info(sender, "disable.single.disabled", fileName);
                        return true;
                    }
                    ScriptLoader.unloadScripts(ScriptLoader.getScripts(scriptFile));
                    try {
                        scripts = SkriptCommand.toggleFiles(scriptFile, false);
                    }
                    catch (IOException e7) {
                        SkriptCommand.error(sender, "disable.folder.io error", scriptFile.getName(), ExceptionUtils.toString(e7));
                        boolean e6 = true;
                        if (timingLogHandler != null) {
                            timingLogHandler.close();
                        }
                        if (logHandler == null) return e6;
                        logHandler.close();
                        return e6;
                    }
                    if (scripts.isEmpty()) {
                        SkriptCommand.info(sender, "disable.folder.empty", scriptFile.getName());
                        boolean e7 = true;
                        return e7;
                    }
                    SkriptCommand.info(sender, "disable.folder.disabled", scriptFile.getName(), scripts.size());
                    return true;
                }
                if (args[0].equalsIgnoreCase("update")) {
                    SkriptUpdater updater = Skript.getInstance().getUpdater();
                    if (updater == null) {
                        Skript.info(sender, String.valueOf(SkriptUpdater.m_internal_error));
                        boolean scripts = true;
                        return scripts;
                    }
                    if (args[1].equalsIgnoreCase("check")) {
                        updater.updateCheck(sender);
                        return true;
                    }
                    if (!args[1].equalsIgnoreCase("changes")) return true;
                    updater.changesCheck(sender);
                    return true;
                }
                if (args[0].equalsIgnoreCase("info")) {
                    SkriptCommand.info(sender, "info.aliases", new Object[0]);
                    SkriptCommand.info(sender, "info.documentation", new Object[0]);
                    SkriptCommand.info(sender, "info.tutorials", new Object[0]);
                    SkriptCommand.info(sender, "info.server", Bukkit.getVersion());
                    SkriptUpdater updater = Skript.getInstance().getUpdater();
                    if (updater != null) {
                        SkriptCommand.info(sender, "info.version", String.valueOf(Skript.getVersion()) + " (" + updater.getCurrentRelease().flavor + ")");
                    } else {
                        SkriptCommand.info(sender, "info.version", Skript.getVersion());
                    }
                    Collection<SkriptAddon> addons = Skript.instance().addons();
                    SkriptCommand.info(sender, "info.addons", addons.isEmpty() ? "None" : "");
                    e7 = addons.iterator();
                    break block107;
                } else {
                    if (args[0].equalsIgnoreCase("gen-docs")) {
                        File templateDir = Documentation.getDocsTemplateDirectory();
                        File outputDir = Documentation.getDocsOutputDirectory();
                        outputDir.mkdirs();
                        Skript.info(sender, "Generating docs...");
                        JSONGenerator.of(Skript.instance()).generate(outputDir.toPath().resolve("docs.json"));
                        if (!templateDir.exists()) {
                            Skript.info(sender, "JSON-only documentation generated!");
                            boolean dependencies = true;
                            return dependencies;
                        }
                        HTMLGenerator htmlGenerator = new HTMLGenerator(templateDir, outputDir);
                        htmlGenerator.generate();
                        Skript.info(sender, "All documentation generated!");
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("test") && TestMode.DEV_MODE) {
                        if (args.length == 1) {
                            scriptFile = TestMode.lastTestFile;
                            if (scriptFile == null) {
                                Skript.error(sender, "No test script has been run yet!");
                                boolean outputDir = true;
                                return outputDir;
                            }
                        } else if (args[1].equalsIgnoreCase("all")) {
                            scriptFile = TestMode.TEST_DIR.toFile();
                        } else {
                            TestMode.lastTestFile = scriptFile = SkriptCommand.getScriptFromArgs(sender, args, TestMode.TEST_DIR.toFile());
                        }
                        if (scriptFile != null && scriptFile.exists()) {
                            timingLogHandler.close();
                            logHandler.close();
                            TestingLogHandler errorCounter = new TestingLogHandler(Level.SEVERE).start();
                            ScriptLoader.loadScripts(scriptFile, (OpenCloseable)errorCounter).thenAccept(scriptInfo -> Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), () -> {
                                String[] lines;
                                Bukkit.getPluginManager().callEvent((Event)new SkriptTestEvent());
                                ScriptLoader.unloadScripts(ScriptLoader.getLoadedScripts());
                                TestResults testResults = TestTracker.collectResults();
                                for (String line : lines = testResults.createReport().split("\n")) {
                                    Skript.info(sender, line);
                                }
                                Skript.info(sender, "Collecting results to " + String.valueOf(TestMode.RESULTS_FILE));
                                String results = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson((Object)testResults);
                                try {
                                    Files.writeString(TestMode.RESULTS_FILE, (CharSequence)results, new OpenOption[0]);
                                }
                                catch (IOException e) {
                                    Skript.exception((Throwable)e, "Failed to write test results.");
                                }
                            }));
                            return true;
                        }
                        Skript.error(sender, "Test script doesn't exist!");
                        boolean outputDir = true;
                        return outputDir;
                    }
                    if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("show")) {
                        SkriptCommand.info(sender, "list.enabled.header", new Object[0]);
                        ScriptLoader.getLoadedScripts().stream().map(script -> script.getConfig().getFileName()).forEach(name -> SkriptCommand.info(sender, "list.enabled.element", name));
                        SkriptCommand.info(sender, "list.disabled.header", new Object[0]);
                        ScriptLoader.getDisabledScripts().stream().flatMap(file -> {
                            if (file.isDirectory()) {
                                return SkriptCommand.getSubFiles(file).stream();
                            }
                            return Arrays.stream(new File[]{file});
                        }).map(File::getPath).map(path -> path.substring(Skript.getInstance().getScriptsFolder().getPath().length() + 1)).forEach(path -> SkriptCommand.info(sender, "list.disabled.element", path));
                        return true;
                    }
                    if (!args[0].equalsIgnoreCase("help")) return true;
                    SKRIPT_COMMAND_HELP.showHelp(sender);
                    return true;
                }
                finally {
                    if (timingLogHandler != null) {
                        timingLogHandler.close();
                    }
                }
                finally {
                    if (logHandler != null) {
                        logHandler.close();
                    }
                }
            }
            while (e7.hasNext()) {
                SkriptAddon addon = e7.next();
                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(addon.source());
                PluginDescriptionFile desc = plugin.getDescription();
                String web = desc.getWebsite();
                Skript.info(sender, " - " + desc.getFullName() + (String)(web != null ? " (" + web + ")" : ""));
            }
            List dependencies = Skript.getInstance().getDescription().getSoftDepend();
            boolean dependenciesFound = false;
            Iterator iterator = dependencies.iterator();
            while (true) {
                if (!iterator.hasNext()) {
                    if (dependenciesFound) return true;
                    SkriptCommand.info(sender, "info.dependencies", "None");
                    return true;
                }
                String dep = (String)iterator.next();
                Plugin plugin = Bukkit.getPluginManager().getPlugin(dep);
                if (plugin == null) continue;
                if (!dependenciesFound) {
                    dependenciesFound = true;
                    SkriptCommand.info(sender, "info.dependencies", "");
                }
                String ver = plugin.getDescription().getVersion();
                Skript.info(sender, " - " + plugin.getName() + " v" + ver);
            }
        }
        catch (Exception e) {
            Skript.exception((Throwable)e, "Exception occurred in Skript's main command", "Used command: /" + label + " " + StringUtils.join(args, " "));
        }
        return true;
    }

    private static List<File> getSubFiles(File file) {
        ArrayList<File> files = new ArrayList<File>();
        if (file.isDirectory()) {
            for (File listFile : file.listFiles(f -> !f.isHidden())) {
                if (listFile.isDirectory()) {
                    files.addAll(SkriptCommand.getSubFiles(listFile));
                    continue;
                }
                if (!listFile.getName().endsWith(".sk")) continue;
                files.add(listFile);
            }
        }
        return files;
    }

    @Nullable
    private static File getScriptFromArgs(CommandSender sender, String[] args) {
        return SkriptCommand.getScriptFromArgs(sender, args, Skript.getInstance().getScriptsFolder());
    }

    @Nullable
    private static File getScriptFromArgs(CommandSender sender, String[] args, File directoryFile) {
        String script = StringUtils.join(args, " ", 1, args.length);
        File f = ScriptLoader.getScriptFromName(script, directoryFile);
        if (f == null) {
            boolean directory = script.endsWith("/") || script.endsWith("\\") || script.endsWith(File.separator);
            Skript.error(sender, (directory ? m_invalid_folder : m_invalid_script).toString(script));
            return null;
        }
        return f;
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    @Nullable
    public static File getScriptFromName(String script) {
        return ScriptLoader.getScriptFromName(script);
    }

    private static File toggleFile(File file, boolean enable) throws IOException {
        if (enable) {
            return FileUtils.move(file, new File(file.getParentFile(), file.getName().substring(ScriptLoader.DISABLED_SCRIPT_PREFIX_LENGTH)), false);
        }
        return FileUtils.move(file, new File(file.getParentFile(), "-" + file.getName()), false);
    }

    private static Set<File> toggleFiles(File folder, boolean enable) throws IOException {
        FileFilter filter = enable ? ScriptLoader.getDisabledScriptsFilter() : ScriptLoader.getLoadedScriptsFilter();
        HashSet<File> changed = new HashSet<File>();
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                changed.addAll(SkriptCommand.toggleFiles(file, enable));
                continue;
            }
            if (!filter.accept(file)) continue;
            String fileName = file.getName();
            changed.add(FileUtils.move(file, new File(file.getParentFile(), (String)(enable ? fileName.substring(ScriptLoader.DISABLED_SCRIPT_PREFIX_LENGTH) : "-" + fileName)), false));
        }
        return changed;
    }

    static {
        if (TestMode.GEN_DOCS || Documentation.isDocsTemplateFound()) {
            SKRIPT_COMMAND_HELP.add("gen-docs");
        }
        if (TestMode.DEV_MODE) {
            SKRIPT_COMMAND_HELP.add("test");
        }
        m_reloaded = new ArgsMessage("skript command.reload.reloaded");
        m_reload_error = new ArgsMessage("skript command.reload.error");
        m_invalid_script = new ArgsMessage("skript command.invalid script");
        m_invalid_folder = new ArgsMessage("skript command.invalid folder");
    }
}

