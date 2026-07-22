/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Server
 *  org.bukkit.World
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.ConsoleCommandSender
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.server.PluginDisableEvent
 *  org.bukkit.event.server.ServerCommandEvent
 *  org.bukkit.event.server.ServerLoadEvent
 *  org.bukkit.event.server.ServerLoadEvent$LoadType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.PluginDescriptionFile
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 *  org.jetbrains.annotations.Unmodifiable
 *  org.junit.After
 *  org.junit.runner.JUnitCore
 *  org.junit.runner.Result
 *  org.junit.runner.notification.Failure
 */
package ch.njol.skript;

import ch.njol.skript.ModernSkriptBridge;
import ch.njol.skript.ScriptLoader;
import ch.njol.skript.ServerPlatform;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.SkriptCommand;
import ch.njol.skript.SkriptCommandTabCompleter;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.SkriptUpdater;
import ch.njol.skript.aliases.Aliases;
import ch.njol.skript.bstats.bukkit.Metrics;
import ch.njol.skript.bukkitutil.BurgerHelper;
import ch.njol.skript.classes.data.BukkitClasses;
import ch.njol.skript.classes.data.BukkitEventValues;
import ch.njol.skript.classes.data.DefaultComparators;
import ch.njol.skript.classes.data.DefaultConverters;
import ch.njol.skript.classes.data.DefaultFunctions;
import ch.njol.skript.classes.data.DefaultOperations;
import ch.njol.skript.classes.data.JavaClasses;
import ch.njol.skript.classes.data.SkriptClasses;
import ch.njol.skript.command.Commands;
import ch.njol.skript.doc.Documentation;
import ch.njol.skript.events.EvtSkript;
import ch.njol.skript.expressions.arithmetic.ExprArithmetic;
import ch.njol.skript.hooks.Hook;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.Statement;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.skript.localization.PluralizingArgsMessage;
import ch.njol.skript.log.BukkitLoggerFilter;
import ch.njol.skript.log.CountingLogHandler;
import ch.njol.skript.log.ErrorDescLogHandler;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.log.TestingLogHandler;
import ch.njol.skript.log.Verbosity;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.registrations.Feature;
import ch.njol.skript.test.runner.EffObjectives;
import ch.njol.skript.test.runner.SkriptAsyncJUnitTest;
import ch.njol.skript.test.runner.SkriptJUnitTest;
import ch.njol.skript.test.runner.SkriptTestEvent;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.skript.test.runner.TestTracker;
import ch.njol.skript.timings.SkriptTimings;
import ch.njol.skript.update.ReleaseManifest;
import ch.njol.skript.update.ReleaseStatus;
import ch.njol.skript.update.UpdateManifest;
import ch.njol.skript.util.Date;
import ch.njol.skript.util.EmptyStacktraceException;
import ch.njol.skript.util.ExceptionUtils;
import ch.njol.skript.util.FileUtils;
import ch.njol.skript.util.Task;
import ch.njol.skript.util.Version;
import ch.njol.skript.util.chat.ChatMessages;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Closeable;
import ch.njol.util.OpenCloseable;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.iterator.CheckedIterator;
import ch.njol.util.coll.iterator.EnumerationIterable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.After;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.bukkit.BukkitModule;
import org.skriptlang.skript.bukkit.SkriptMetrics;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.log.runtime.BukkitRuntimeErrorConsumer;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.bukkit.text.TextComponentParser;
import org.skriptlang.skript.common.CommonModule;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.experiment.ExperimentRegistry;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.PropertyRegistry;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.lang.structure.StructureInfo;
import org.skriptlang.skript.log.runtime.RuntimeErrorManager;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.ClassLoader;

public final class Skript
extends JavaPlugin
implements Listener {
    @Nullable
    private static Skript instance = null;
    private static @UnknownNullability org.skriptlang.skript.Skript skript = null;
    private static @UnknownNullability org.skriptlang.skript.Skript unmodifiableSkript = null;
    private static boolean disabled = false;
    private static boolean partDisabled = false;
    @Nullable
    private SkriptUpdater updater;
    private static Version minecraftVersion = new Version(666);
    private static Version UNKNOWN_VERSION = new Version(666);
    private static ServerPlatform serverPlatform = ServerPlatform.BUKKIT_UNKNOWN;
    @Nullable
    private static Version version = null;
    @Deprecated(since="2.9.0", forRemoval=true)
    private static @UnknownNullability ExperimentRegistry experimentRegistry;
    public static final Message m_invalid_reload;
    public static final Message m_finished_loading;
    public static final Message m_no_errors;
    public static final Message m_no_scripts;
    private static final PluralizingArgsMessage m_scripts_loaded;
    private static final Message WARNING_MESSAGE;
    private static final Message RESTART_MESSAGE;
    private static final Set<Class<? extends Hook<?>>> disabledHookRegistrations;
    private static boolean finishedLoadingHooks;
    private File scriptsFolder;
    @Nullable
    static Metrics metrics;
    private static final Collection<Closeable> closeOnDisable;
    private static final boolean IS_STOPPING_EXISTS;
    @Nullable
    private static Method IS_RUNNING;
    @Nullable
    private static Object MC_SERVER;
    public static final String SCRIPTSFOLDER = "scripts";
    public static final double EPSILON = 1.0E-10;
    public static final double EPSILON_MULT = 1.00001;
    public static final int MAXBLOCKID = 255;
    public static final int MAXDATAVALUE = 65535;
    public static final Thread.UncaughtExceptionHandler UEH;
    private static boolean acceptRegistrations;
    @Deprecated(since="2.10.0", forRemoval=true)
    private static final Set<SkriptAddon> addons;
    @Deprecated(since="2.10.0", forRemoval=true)
    @Nullable
    private static SkriptAddon addon;
    private static final String EXCEPTION_PREFIX = "#!#! ";
    private static Map<String, PluginDescriptionFile> pluginPackages;
    private static boolean checkedPlugins;
    private static boolean tainted;
    private static boolean errored;
    private static final Message SKRIPT_PREFIX_MESSAGE;

    public static Skript getInstance() {
        if (instance == null) {
            throw new IllegalStateException();
        }
        return instance;
    }

    public static org.skriptlang.skript.Skript instance() {
        if (unmodifiableSkript == null) {
            throw new SkriptAPIException("Skript is still initializing");
        }
        return unmodifiableSkript;
    }

    public Skript() throws IllegalStateException {
        if (instance != null) {
            throw new IllegalStateException("Cannot create multiple instances of Skript!");
        }
        instance = this;
    }

    public static void updateMinecraftVersion() {
        String bukkitV = Bukkit.getBukkitVersion();
        Matcher m = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?").matcher(bukkitV);
        minecraftVersion = !m.find() ? new Version(666, 0, 0) : new Version(m.group());
    }

    public static Version getVersion() {
        Version v = version;
        if (v == null) {
            throw new IllegalStateException();
        }
        return v;
    }

    public static String getWarningMessage() {
        return WARNING_MESSAGE.getValueOrDefault("It appears that /reload or another plugin reloaded Skript. This is not supported behaviour and may cause issues.");
    }

    public static String getRestartMessage() {
        return RESTART_MESSAGE.getValueOrDefault("Please consider restarting the server instead.");
    }

    public static ServerPlatform getServerPlatform() {
        if (Skript.classExists("net.glowstone.GlowServer")) {
            return ServerPlatform.BUKKIT_GLOWSTONE;
        }
        if (Skript.classExists("co.aikar.timings.Timings")) {
            return ServerPlatform.BUKKIT_PAPER;
        }
        if (Skript.classExists("org.spigotmc.SpigotConfig")) {
            return ServerPlatform.BUKKIT_SPIGOT;
        }
        if (Skript.classExists("org.bukkit.craftbukkit.CraftServer") || Skript.classExists("org.bukkit.craftbukkit.Main")) {
            return ServerPlatform.BUKKIT_CRAFTBUKKIT;
        }
        return ServerPlatform.BUKKIT_UNKNOWN;
    }

    /*
     * Enabled aggressive block sorting
     */
    private static boolean checkServerPlatform() {
        String bukkitV = Bukkit.getBukkitVersion();
        Matcher m = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?").matcher(bukkitV);
        if (!m.find()) {
            Skript.error("The Bukkit version '" + bukkitV + "' does not contain a version number which is required for Skript to enable or disable certain features. Skript will still work, but you might get random errors if you use features that are not available in your version of Bukkit.");
            minecraftVersion = new Version(666, 0, 0);
        } else {
            minecraftVersion = new Version(m.group());
        }
        Skript.debug("Loading for Minecraft " + String.valueOf(minecraftVersion));
        if (!Skript.isRunningMinecraft(1, 9)) {
            Skript.error("This version of Skript does not work with Minecraft " + String.valueOf(minecraftVersion) + " and requires Minecraft 1.9.4+");
            Skript.error("You probably want Skript 2.2 or 2.1 (Google to find where to get them)");
            Skript.error("Note that those versions are, of course, completely unsupported!");
            return false;
        }
        serverPlatform = Skript.getServerPlatform();
        Skript.debug("Server platform: " + String.valueOf((Object)serverPlatform));
        if (Skript.serverPlatform.works) {
            if (Skript.serverPlatform.supported) return true;
            Skript.warning("This server platform (" + Skript.serverPlatform.name + ") is not supported by Skript.");
            Skript.warning("It will still probably work, but if it does not, you are on your own.");
            Skript.warning("Skript officially supports Paper and Spigot.");
            return true;
        }
        Skript.error("It seems that this server platform (" + Skript.serverPlatform.name + ") does not work with Skript.");
        if (SkriptConfig.allowUnsafePlatforms.value().booleanValue()) {
            Skript.error("However, you have chosen to ignore this. Skript will probably still not work.");
            return true;
        }
        Skript.error("To prevent potentially unsafe behaviour, Skript has been disabled.");
        Skript.error("You may re-enable it by adding a configuration option 'allow unsafe platforms: true'");
        Skript.error("Note that it is unlikely that Skript works correctly even if you do so.");
        Skript.error("A better idea would be to install Paper or Spigot in place of your current server.");
        return false;
    }

    public static boolean isHookEnabled(Class<? extends Hook<?>> hook) {
        return !disabledHookRegistrations.contains(hook);
    }

    public static boolean isFinishedLoadingHooks() {
        return finishedLoadingHooks;
    }

    @SafeVarargs
    public static void disableHookRegistration(Class<? extends Hook<?>> ... hooks) {
        if (finishedLoadingHooks) {
            throw new SkriptAPIException("Disabling hooks is not possible after Skript has been enabled!");
        }
        Collections.addAll(disabledHookRegistrations, hooks);
    }

    public static ExperimentRegistry experiments() {
        return experimentRegistry;
    }

    public File getScriptsFolder() {
        if (!this.scriptsFolder.isDirectory()) {
            this.scriptsFolder.mkdirs();
        }
        return this.scriptsFolder;
    }

    public static RuntimeErrorManager getRuntimeErrorManager() {
        return RuntimeErrorManager.getInstance();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void onEnable() {
        int pauseThreshold;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        if (disabled) {
            Skript.error(m_invalid_reload.toString());
            this.setEnabled(false);
            return;
        }
        this.handleJvmArguments();
        version = new Version(this.getDescription().getVersion());
        try {
            this.updater = new SkriptUpdater();
        }
        catch (Exception e) {
            Skript.exception((Throwable)e, "Update checker could not be initialized.");
        }
        if (!this.getDataFolder().isDirectory()) {
            this.getDataFolder().mkdirs();
        }
        this.scriptsFolder = new File(this.getDataFolder(), SCRIPTSFOLDER);
        File config = new File(this.getDataFolder(), "config.sk");
        File features = new File(this.getDataFolder(), "features.sk");
        File lang = new File(this.getDataFolder(), "lang");
        File aliasesFolder = new File(this.getDataFolder(), "aliases");
        if (!(this.scriptsFolder.isDirectory() && config.exists() && features.exists() && lang.exists() && aliasesFolder.exists())) {
            ZipFile f = null;
            try {
                boolean populateExamples = false;
                if (!this.scriptsFolder.isDirectory()) {
                    if (!this.scriptsFolder.mkdirs()) {
                        throw new IOException("Could not create the directory " + String.valueOf(this.scriptsFolder));
                    }
                    populateExamples = true;
                }
                boolean populateLanguageFiles = false;
                if (!lang.isDirectory()) {
                    if (!lang.mkdirs()) {
                        throw new IOException("Could not create the directory " + String.valueOf(lang));
                    }
                    populateLanguageFiles = true;
                }
                if (!aliasesFolder.isDirectory() && !aliasesFolder.mkdirs()) {
                    throw new IOException("Could not create the directory " + String.valueOf(aliasesFolder));
                }
                f = new ZipFile(this.getFile());
                for (ZipEntry zipEntry : new EnumerationIterable<ZipEntry>(f.entries())) {
                    if (zipEntry.isDirectory()) continue;
                    File saveTo = null;
                    if (populateExamples && zipEntry.getName().startsWith("scripts/")) {
                        fileName = zipEntry.getName().substring(zipEntry.getName().indexOf("/") + 1);
                        if (!((String)fileName).startsWith("-")) {
                            fileName = "-" + (String)fileName;
                        }
                        saveTo = new File(this.scriptsFolder, (String)fileName);
                    } else if (populateLanguageFiles && zipEntry.getName().startsWith("lang/") && !zipEntry.getName().endsWith("default.lang")) {
                        fileName = zipEntry.getName().substring(zipEntry.getName().lastIndexOf("/") + 1);
                        saveTo = new File(lang, (String)fileName);
                    } else if (zipEntry.getName().equals("config.sk")) {
                        if (!config.exists()) {
                            saveTo = config;
                        }
                    } else if (zipEntry.getName().startsWith("features.sk") && !features.exists()) {
                        saveTo = features;
                    }
                    if (saveTo == null) continue;
                    try (InputStream in = f.getInputStream(zipEntry);){
                        assert (in != null);
                        FileUtils.save(in, saveTo);
                    }
                }
                Skript.info("Successfully generated the config and the example scripts.");
            }
            catch (ZipException populateExamples) {
            }
            catch (IOException e) {
                Skript.error("Error generating the default files: " + ExceptionUtils.toString(e));
            }
            finally {
                if (f != null) {
                    try {
                        f.close();
                    }
                    catch (IOException e) {}
                }
            }
        }
        skript = org.skriptlang.skript.Skript.of(((Object)((Object)this)).getClass(), this.getName());
        unmodifiableSkript = new ModernSkriptBridge.SpecialUnmodifiableSkript(skript);
        skript.localizer().setSourceDirectories("lang", this.getDataFolder().getAbsolutePath() + File.separatorChar + "lang");
        Skript.getAddonInstance();
        experimentRegistry = new ExperimentRegistry(this);
        Feature.registerAll(Skript.getAddonInstance(), experimentRegistry);
        skript.storeRegistry(PropertyRegistry.class, new PropertyRegistry(this));
        Property.registerDefaultProperties();
        final EventValueRegistry eventValueRegistry = EventValueRegistry.empty(this);
        skript.storeRegistry(EventValueRegistry.class, eventValueRegistry);
        EventValues.setEventValueRegistry(eventValueRegistry);
        new JavaClasses();
        new SkriptClasses();
        new BukkitClasses();
        SkriptConfig.load();
        if (!Skript.checkServerPlatform()) {
            disabled = true;
            this.setEnabled(false);
            return;
        }
        if (Skript.methodExists(Server.class, "getPauseWhenEmptyTime", new Class[0]) && (pauseThreshold = this.getServer().getPauseWhenEmptyTime()) > -1) {
            Skript.warning("Minecraft server pausing is enabled!");
            Skript.warning("Scripts that interact with the world or entities may not work as intended when the server is paused and may crash your server.");
            Skript.warning("Consider setting 'pause-when-empty-seconds' to -1 in server.properties to make sure you don't encounter any issues.");
        }
        SkriptConfig.eventRegistry().register(SkriptConfig.ReloadEvent.class, RuntimeErrorManager::refresh);
        RuntimeErrorManager.refresh();
        Skript.getRuntimeErrorManager().addConsumer(new BukkitRuntimeErrorConsumer());
        if (TestMode.VERBOSITY != null) {
            SkriptLogger.setVerbosity(Verbosity.valueOf(TestMode.VERBOSITY));
        }
        if (this.updater != null) {
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            assert (console != null);
            assert (this.updater != null);
            this.updater.updateCheck((CommandSender)console);
        }
        PluginCommand skriptCommand = this.getCommand("skript");
        assert (skriptCommand != null);
        skriptCommand.setExecutor((CommandExecutor)new SkriptCommand());
        skriptCommand.setTabCompleter((TabCompleter)new SkriptCommandTabCompleter());
        AddonModule legacyModule = new AddonModule(){

            @Override
            public void init(org.skriptlang.skript.addon.SkriptAddon addon) {
                new DefaultComparators();
                new DefaultConverters();
                ChatMessages.registerListeners();
            }

            @Override
            public void load(org.skriptlang.skript.addon.SkriptAddon addon) {
                try {
                    Skript.getAddonInstance().loadClasses("ch.njol.skript", "conditions", "effects", "events", "expressions", "entity", "literals", "sections", "structures");
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
                BukkitEventValues.register(eventValueRegistry);
                new DefaultFunctions();
                new DefaultOperations();
            }

            @Override
            public String name() {
                return "legacy";
            }
        };
        try {
            skript.loadModules(legacyModule, new CommonModule(), new BukkitModule());
        }
        catch (Exception e) {
            Skript.exception((Throwable)e, "Failed to load one of Skript's modules: " + e.getLocalizedMessage());
            this.setEnabled(false);
            return;
        }
        final CompletableFuture<Boolean> aliases = Aliases.loadAsync();
        Commands.registerListeners();
        if (Skript.logNormal()) {
            Skript.info(" " + Language.get("skript.copyright"));
        }
        final long l = Skript.testing() ? ((World)Bukkit.getWorlds().get(0)).getFullTime() : 0L;
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, new Runnable(){
            final /* synthetic */ Skript this$0;
            {
                this.this$0 = this$0;
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                Object c;
                assert (((World)Bukkit.getWorlds().get(0)).getFullTime() == l);
                try (JarFile jar = new JarFile(this.this$0.getFile());){
                    for (JarEntry e : new EnumerationIterable<JarEntry>(jar.entries())) {
                        if (!e.getName().startsWith("ch/njol/skript/hooks/") || !e.getName().endsWith("Hook.class") || StringUtils.count(e.getName(), '/') > 5) continue;
                        c = e.getName().replace('/', '.').substring(0, e.getName().length() - ".class".length());
                        try {
                            Class<?> hook = Class.forName((String)c, true, this.this$0.getClassLoader());
                            if (!Hook.class.isAssignableFrom(hook) || Modifier.isAbstract(hook.getModifiers()) || !Skript.isHookEnabled(hook)) continue;
                            hook.getDeclaredConstructor(new Class[0]).setAccessible(true);
                            hook.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
                        }
                        catch (ClassNotFoundException ex) {
                            Skript.exception((Throwable)ex, "Cannot load class " + (String)c);
                        }
                        catch (ExceptionInInitializerError err) {
                            Skript.exception(err.getCause(), "Class " + (String)c + " generated an exception while loading");
                        }
                        catch (Exception ex) {
                            Skript.exception((Throwable)ex, "Exception initializing hook: " + (String)c);
                        }
                    }
                }
                catch (IOException e) {
                    Skript.error("Error while loading plugin hooks" + (String)(e.getLocalizedMessage() == null ? "" : ": " + e.getLocalizedMessage()));
                    Skript.exception((Throwable)e, new String[0]);
                }
                finishedLoadingHooks = true;
                try {
                    aliases.get();
                }
                catch (InterruptedException | ExecutionException e) {
                    Skript.exception((Throwable)e, "Could not load aliases concurrently");
                }
                if (TestMode.ENABLED) {
                    Skript.info("Preparing Skript for testing...");
                    tainted = true;
                    try {
                        Skript.getAddonInstance().loadClasses("ch.njol.skript.test.runner", new String[0]);
                        if (TestMode.JUNIT) {
                            Skript.getAddonInstance().loadClasses("org.skriptlang.skript.test.junit.registration", new String[0]);
                        }
                    }
                    catch (IOException e) {
                        Skript.exception("Failed to load testing environment.");
                        Bukkit.getServer().shutdown();
                    }
                }
                Skript.stopAcceptingRegistrations();
                Documentation.generate();
                if (Skript.logNormal()) {
                    Skript.info("Loading variables...");
                }
                long vls = System.currentTimeMillis();
                1 h = SkriptLogger.startLogHandler(new ErrorDescLogHandler(this){

                    @Override
                    public LogHandler.LogResult log(LogEntry entry) {
                        super.log(entry);
                        if (entry.level.intValue() >= Level.SEVERE.intValue()) {
                            Skript.logEx(entry.message);
                            return LogHandler.LogResult.DO_NOT_LOG;
                        }
                        return LogHandler.LogResult.LOG;
                    }

                    @Override
                    protected void beforeErrors() {
                        Skript.logEx();
                        Skript.logEx("===!!!=== Skript variable load error ===!!!===");
                        Skript.logEx("Unable to load (all) variables:");
                    }

                    @Override
                    protected void afterErrors() {
                        Skript.logEx();
                        Skript.logEx("Skript will work properly, but old variables might not be available at all and new ones may or may not be saved until Skript is able to create a backup of the old file and/or is able to connect to the database (which requires a restart of Skript)!");
                        Skript.logEx();
                    }
                });
                try {
                    c = new CountingLogHandler(SkriptLogger.SEVERE).start();
                    try {
                        if (!Variables.load() && ((CountingLogHandler)c).getCount() == 0) {
                            Skript.error("(no information available)");
                        }
                    }
                    finally {
                        if (c != null) {
                            ((LogHandler)c).close();
                        }
                    }
                }
                finally {
                    h.stop();
                }
                long vld = System.currentTimeMillis() - vls;
                if (Skript.logNormal()) {
                    Skript.info("Loaded " + Variables.numVariables() + " variables in " + (double)(vld / 100L) / 10.0 + " seconds");
                }
                Skript.debug("Early init done");
                if (TestMode.ENABLED) {
                    if (TestMode.DEV_MODE) {
                        this.this$0.runTests();
                    } else {
                        World world = (World)Bukkit.getWorlds().get(0);
                        world.setSpawnLocation(0, 0, 0);
                        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), () -> {
                            world.addPluginChunkTicket(0, 0, (Plugin)Skript.getInstance());
                            world.addPluginChunkTicket(100, 100, (Plugin)Skript.getInstance());
                            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), () -> this.this$0.runTests(), 100L);
                        }, 5L);
                    }
                }
                metrics = new Metrics((Plugin)Skript.getInstance(), 722);
                SkriptMetrics.setupMetrics(metrics);
                Date start = new Date();
                CountingLogHandler logHandler = new CountingLogHandler(Level.SEVERE);
                File scriptsFolder = this.this$0.getScriptsFolder();
                ScriptLoader.updateDisabledScripts(scriptsFolder.toPath());
                ScriptLoader.loadScripts(scriptsFolder, (OpenCloseable)logHandler).thenAccept(scriptInfo -> {
                    try {
                        if (logHandler.getCount() == 0) {
                            Skript.info(m_no_errors.toString());
                        }
                        if (scriptInfo.files == 0) {
                            Skript.warning(m_no_scripts.toString());
                        }
                        if (Skript.logNormal() && scriptInfo.files > 0) {
                            Skript.info(m_scripts_loaded.toString(scriptInfo.files, scriptInfo.structures, start.difference(new Date())));
                        }
                        Skript.info(m_finished_loading.toString());
                        if (!ScriptLoader.isAsync()) {
                            EvtSkript.onSkriptStart();
                            Filter filter = record -> {
                                if (record == null) {
                                    return false;
                                }
                                return record.getMessage() == null || !record.getMessage().toLowerCase(Locale.ENGLISH).startsWith("can't keep up!");
                            };
                            BukkitLoggerFilter.addFilter(filter);
                            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this.this$0, () -> BukkitLoggerFilter.removeFilter(filter), 1L);
                        } else {
                            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this.this$0, EvtSkript::onSkriptStart);
                        }
                    }
                    catch (Exception e) {
                        throw Skript.exception((Throwable)e, new String[0]);
                    }
                });
            }
        });
        if (!TestMode.ENABLED) {
            Bukkit.getPluginManager().registerEvents((Listener)new JoinUpdateNotificationListener(), (Plugin)this);
        }
        Bukkit.getPluginManager().registerEvents((Listener)new ServerReloadListener(), (Plugin)this);
        SkriptTimings.setSkript(this);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void runTests() {
        Skript.info("Skript testing environment enabled, starting...");
        AtomicLong shutdownDelay = new AtomicLong(0L);
        ArrayList<Class> asyncTests = new ArrayList<Class>();
        CompletableFuture<Object> onAsyncComplete = CompletableFuture.completedFuture(null);
        if (TestMode.GEN_DOCS) {
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)"skript gen-docs");
        } else {
            if (TestMode.DEV_MODE) {
                Skript.info("Test development mode enabled. Test scripts are at " + String.valueOf(TestMode.TEST_DIR));
                return;
            }
            Skript.info("Loading all tests from " + String.valueOf(TestMode.TEST_DIR));
            TestingLogHandler errorCounter = new TestingLogHandler(Level.SEVERE);
            try {
                errorCounter.start();
                ScriptLoader.loadScripts(new File(this.getScriptsFolder(), "-examples" + File.separator), (OpenCloseable)errorCounter);
                ScriptLoader.unloadScripts(ScriptLoader.getLoadedScripts());
                ScriptLoader.loadScripts(TestMode.TEST_DIR.toFile(), (OpenCloseable)errorCounter);
            }
            finally {
                errorCounter.stop();
            }
            Bukkit.getPluginManager().callEvent((Event)new SkriptTestEvent());
            if (errorCounter.getCount() > 0) {
                TestTracker.testStarted("parse scripts");
                TestTracker.testFailed(errorCounter.getCount() + " error(s) found");
            }
            if (errored) {
                TestTracker.testStarted("run scripts");
                TestTracker.testFailed("exception was thrown during execution");
            }
            if (TestMode.JUNIT) {
                AtomicLong milliseconds = new AtomicLong(0L);
                AtomicLong tests = new AtomicLong(0L);
                AtomicLong fails = new AtomicLong(0L);
                AtomicLong ignored = new AtomicLong(0L);
                AtomicLong size = new AtomicLong(0L);
                Skript.info("Running sync JUnit tests...");
                try {
                    HashSet classes = new HashSet();
                    ClassLoader.builder().addSubPackages("org.skriptlang.skript", "ch.njol.skript").filter(fqn -> fqn.endsWith("Test")).initialize(true).deep(true).forEachClass(clazz -> {
                        if (clazz.isAnonymousClass() || clazz.isLocalClass()) {
                            return;
                        }
                        classes.add(clazz);
                    }).build().loadClasses(Skript.class, this.getFile());
                    classes.remove(SkriptJUnitTest.class);
                    classes.remove(SkriptAsyncJUnitTest.class);
                    size.set(classes.size());
                    for (Class clazz2 : classes) {
                        if (SkriptAsyncJUnitTest.class.isAssignableFrom(clazz2)) {
                            asyncTests.add(clazz2);
                            continue;
                        }
                        this.runTest(clazz2, shutdownDelay, tests, milliseconds, ignored, fails);
                    }
                }
                catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                    Skript.exception((Throwable)e, "Failed to initalize test JUnit classes.");
                }
                if (ignored.get() > 0L) {
                    Skript.warning("There were " + String.valueOf(ignored) + " ignored test cases! This can mean they are not properly setup in order in that class!");
                }
                onAsyncComplete = CompletableFuture.runAsync(() -> {
                    Skript.info("Running async JUnit tests...");
                    try {
                        for (Class clazz : asyncTests) {
                            this.runTest(clazz, shutdownDelay, tests, milliseconds, ignored, fails);
                        }
                    }
                    catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
                        Skript.exception((Throwable)e, "Failed to initalize test JUnit classes.");
                    }
                    if (ignored.get() > 0L) {
                        Skript.warning("There were " + String.valueOf(ignored) + " ignored test cases! This can mean they are not properly setup in order in that class!");
                    }
                    Skript.info("Completed " + String.valueOf(tests) + " JUnit tests in " + String.valueOf(size) + " classes with " + String.valueOf(fails) + " failures in " + String.valueOf(milliseconds) + " milliseconds.");
                });
            }
        }
        onAsyncComplete.thenRun(() -> {
            double display;
            Skript.info("Testing done, shutting down the server in " + display + " second" + ((display = (double)shutdownDelay.get() / 20.0) == 1.0 ? "" : "s") + "...");
            Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
                Skript.info("Shutting down server.");
                if (TestMode.JUNIT && !EffObjectives.isJUnitComplete()) {
                    EffObjectives.fail();
                }
                Skript.info("Collecting results to " + String.valueOf(TestMode.RESULTS_FILE));
                String results = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson((Object)TestTracker.collectResults());
                try {
                    Files.write(TestMode.RESULTS_FILE, results.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
                }
                catch (IOException e) {
                    Skript.exception((Throwable)e, "Failed to write test results.");
                }
                Bukkit.getScheduler().runTaskLater((Plugin)this, () -> Bukkit.getServer().shutdown(), 1L);
            }, shutdownDelay.get());
        });
    }

    private void runTest(Class<?> clazz, AtomicLong shutdownDelay, AtomicLong tests, AtomicLong milliseconds, AtomicLong ignored, AtomicLong fails) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String test = clazz.getName();
        SkriptJUnitTest.setCurrentJUnitTest(test);
        SkriptJUnitTest.setShutdownDelay(0L);
        Skript.info("Running JUnit test '" + test + "'");
        Result result = JUnitCore.runClasses((Class[])new Class[]{clazz});
        TestTracker.testStarted("JUnit: '" + test + "'");
        boolean overrides = false;
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(After.class)) continue;
            if (SkriptJUnitTest.getShutdownDelay() > 1L) {
                Skript.warning("Methods annotated with @After in happen instantaneously, and '" + test + "' requires a delay. Do test cleanup in the junit script file or 'cleanup' method.");
            }
            if (!method.getName().equals("cleanup")) continue;
            overrides = true;
        }
        if (SkriptJUnitTest.getShutdownDelay() > 1L && !overrides) {
            Skript.error("The JUnit class '" + test + "' does not override the method 'cleanup', thus the test data will instantly be cleaned up despite requiring a longer shutdown time: " + SkriptJUnitTest.getShutdownDelay());
        }
        shutdownDelay.set(Math.max(shutdownDelay.get(), SkriptJUnitTest.getShutdownDelay()));
        tests.getAndAdd(result.getRunCount());
        milliseconds.getAndAdd(result.getRunTime());
        ignored.getAndAdd(result.getIgnoreCount());
        fails.getAndAdd(result.getFailureCount());
        for (Failure failure : result.getFailures()) {
            String message = failure.getMessage() == null ? "" : " " + failure.getMessage();
            TestTracker.JUnitTestFailed(test, message);
            Skript.exception(failure.getException(), "JUnit test '" + failure.getTestHeader() + " failed.");
        }
        if (SkriptJUnitTest.class.isAssignableFrom(clazz) && !SkriptAsyncJUnitTest.class.isAssignableFrom(clazz)) {
            ((SkriptJUnitTest)clazz.getConstructor(new Class[0]).newInstance(new Object[0])).cleanup();
        }
        SkriptJUnitTest.clearJUnitTest();
    }

    private void handleJvmArguments() {
        Path folder = this.getDataFolder().toPath();
        String burgerEnabled = System.getProperty("skript.burger.enable");
        if (burgerEnabled != null) {
            String burgerInput;
            tainted = true;
            String version = System.getProperty("skript.burger.version");
            if (version == null) {
                String inputFile = System.getProperty("skript.burger.file");
                if (inputFile == null) {
                    Skript.exception("burger enabled but skript.burger.file not provided");
                    return;
                }
                try {
                    burgerInput = new String(Files.readAllBytes(Paths.get(inputFile, new String[0])), StandardCharsets.UTF_8);
                }
                catch (IOException e) {
                    Skript.exception((Throwable)e, new String[0]);
                    return;
                }
            }
            try {
                Path data = folder.resolve("burger-" + version + ".json");
                if (!Files.exists(data, new LinkOption[0])) {
                    URL url = new URL("https://pokechu22.github.io/Burger/" + version + ".json");
                    try (InputStream is = url.openStream();){
                        Files.copy(is, data, new CopyOption[0]);
                    }
                }
                burgerInput = new String(Files.readAllBytes(data), StandardCharsets.UTF_8);
            }
            catch (IOException e) {
                Skript.exception((Throwable)e, new String[0]);
                return;
            }
            try {
                BurgerHelper burger = new BurgerHelper(burgerInput);
                Map<String, Material> materials = burger.mapMaterials();
                Map<Integer, Material> ids = BurgerHelper.mapIds();
                Gson gson = new Gson();
                Files.write(folder.resolve("materials_mappings.json"), gson.toJson(materials).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
                Files.write(folder.resolve("id_mappings.json"), gson.toJson(ids).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
            }
            catch (IOException e) {
                Skript.exception((Throwable)e, new String[0]);
            }
        }
    }

    public static Version getMinecraftVersion() {
        return minecraftVersion;
    }

    public static boolean isRunningCraftBukkit() {
        return serverPlatform == ServerPlatform.BUKKIT_CRAFTBUKKIT;
    }

    public static boolean isRunningMinecraft(int major, int minor) {
        if (minecraftVersion.compareTo(UNKNOWN_VERSION) == 0) {
            Skript.updateMinecraftVersion();
        }
        return minecraftVersion.compareTo(major, minor) >= 0;
    }

    public static boolean isRunningMinecraft(int major, int minor, int revision) {
        if (minecraftVersion.compareTo(UNKNOWN_VERSION) == 0) {
            Skript.updateMinecraftVersion();
        }
        return minecraftVersion.compareTo(major, minor, revision) >= 0;
    }

    public static boolean isRunningMinecraft(Version v) {
        if (minecraftVersion.compareTo(UNKNOWN_VERSION) == 0) {
            Skript.updateMinecraftVersion();
        }
        return minecraftVersion.compareTo(v) >= 0;
    }

    public static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean methodExists(Class<?> c, String methodName, Class<?> ... parameterTypes) {
        try {
            c.getDeclaredMethod(methodName, parameterTypes);
            return true;
        }
        catch (NoSuchMethodException e) {
            return false;
        }
        catch (SecurityException e) {
            return false;
        }
    }

    public static boolean methodExists(Class<?> c, String methodName, Class<?>[] parameterTypes, Class<?> returnType) {
        try {
            Method m = c.getDeclaredMethod(methodName, parameterTypes);
            return m.getReturnType() == returnType;
        }
        catch (NoSuchMethodException e) {
            return false;
        }
        catch (SecurityException e) {
            return false;
        }
    }

    public static boolean fieldExists(Class<?> c, String fieldName) {
        try {
            c.getDeclaredField(fieldName);
            return true;
        }
        catch (NoSuchFieldException e) {
            return false;
        }
        catch (SecurityException e) {
            return false;
        }
    }

    @Nullable
    public static Metrics getMetrics() {
        return metrics;
    }

    public static void closeOnDisable(Closeable closeable) {
        closeOnDisable.add(closeable);
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Plugin plugin = event.getPlugin();
        PluginDescriptionFile descriptionFile = plugin.getDescription();
        if ((descriptionFile.getDepend().contains("Skript") || descriptionFile.getSoftDepend().contains("Skript")) && !this.isServerRunning()) {
            this.beforeDisable();
        }
    }

    private boolean isServerRunning() {
        if (IS_STOPPING_EXISTS) {
            return !Bukkit.getServer().isStopping();
        }
        try {
            return (Boolean)IS_RUNNING.invoke(MC_SERVER, new Object[0]);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void beforeDisable() {
        partDisabled = true;
        EvtSkript.onSkriptStop();
        ScriptLoader.unloadScripts(ScriptLoader.getLoadedScripts());
    }

    public void onDisable() {
        if (disabled) {
            return;
        }
        disabled = true;
        if (!partDisabled) {
            this.beforeDisable();
        }
        Bukkit.getScheduler().cancelTasks((Plugin)this);
        for (Closeable c : closeOnDisable) {
            try {
                c.close();
            }
            catch (Exception e) {
                Skript.exception((Throwable)e, "An error occurred while shutting down.", "This might or might not cause any issues.");
            }
        }
        experimentRegistry = null;
    }

    public static void outdatedError() {
        Skript.error("Skript v" + Skript.getInstance().getDescription().getVersion() + " is not fully compatible with Bukkit " + Bukkit.getVersion() + ". Some feature(s) will be broken until you update Skript.");
    }

    public static void outdatedError(Exception e) {
        Skript.outdatedError();
        if (Skript.testing()) {
            e.printStackTrace();
        }
    }

    public static String toString(double n) {
        return StringUtils.toString(n, SkriptConfig.numberAccuracy.value());
    }

    public static Thread newThread(Runnable r, String name) {
        Thread t = new Thread(r, name);
        t.setUncaughtExceptionHandler(UEH);
        return t;
    }

    public static boolean isAcceptRegistrations() {
        if (instance == null) {
            throw new IllegalStateException("Skript was never loaded");
        }
        return acceptRegistrations && instance.isEnabled();
    }

    public static void checkAcceptRegistrations() {
        if (!Skript.isAcceptRegistrations() && !Skript.testing()) {
            throw new SkriptAPIException("Registration can only be done during plugin initialization");
        }
    }

    private static void stopAcceptingRegistrations() {
        Converters.createChainedConverters();
        ExprArithmetic.registerExpression();
        acceptRegistrations = false;
        Classes.onRegistrationsStop();
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static SkriptAddon registerAddon(JavaPlugin plugin) {
        Skript.checkAcceptRegistrations();
        SkriptAddon addon = new SkriptAddon(plugin);
        addons.add(addon);
        return addon;
    }

    @Deprecated(since="2.14", forRemoval=true)
    @Nullable
    public static SkriptAddon getAddon(JavaPlugin plugin) {
        if (plugin == Skript.getInstance()) {
            return Skript.getAddonInstance();
        }
        for (SkriptAddon addon : Skript.getAddons()) {
            if (addon.plugin != plugin) continue;
            return addon;
        }
        return null;
    }

    @Deprecated(since="2.14", forRemoval=true)
    @Nullable
    public static SkriptAddon getAddon(String name) {
        if (name.equals(Skript.getInstance().getName())) {
            return Skript.getAddonInstance();
        }
        for (SkriptAddon addon : Skript.getAddons()) {
            if (!addon.getName().equals(name)) continue;
            return addon;
        }
        return null;
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static @Unmodifiable Collection<SkriptAddon> getAddons() {
        HashSet<SkriptAddon> addons = new HashSet<SkriptAddon>(Skript.addons);
        addons.addAll(Skript.instance().addons().stream().filter(addon -> addons.stream().noneMatch(oldAddon -> oldAddon.name().equals(addon.name()))).map(SkriptAddon::fromModern).collect(Collectors.toSet()));
        return Collections.unmodifiableCollection(addons);
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static SkriptAddon getAddonInstance() {
        if (addon == null) {
            addon = SkriptAddon.fromModern(Skript.instance());
        }
        return addon;
    }

    @Deprecated(since="2.14", forRemoval=true)
    @ApiStatus.Internal
    public static Origin getSyntaxOrigin(Class<?> source) {
        JavaPlugin plugin;
        try {
            plugin = JavaPlugin.getProvidingPlugin(source);
        }
        catch (IllegalArgumentException e) {
            return Origin.UNKNOWN;
        }
        SkriptAddon addon = Skript.getAddon(plugin);
        if (addon != null) {
            return Origin.of(addon);
        }
        return Origin.UNKNOWN;
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <E extends Condition> void registerCondition(Class<E> conditionClass, String ... patterns) throws IllegalArgumentException {
        Skript.registerCondition(conditionClass, Condition.ConditionType.COMBINED, patterns);
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <E extends Condition> void registerCondition(Class<E> conditionClass, Condition.ConditionType type, String ... patterns) throws IllegalArgumentException {
        Skript.checkAcceptRegistrations();
        skript.syntaxRegistry().register(SyntaxRegistry.CONDITION, SyntaxInfo.builder(conditionClass).priority(type.priority()).origin(Skript.getSyntaxOrigin(conditionClass)).addPatterns(patterns).build());
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <E extends Effect> void registerEffect(Class<E> effectClass, String ... patterns) throws IllegalArgumentException {
        Skript.checkAcceptRegistrations();
        skript.syntaxRegistry().register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(effectClass).origin(Skript.getSyntaxOrigin(effectClass)).addPatterns(patterns).build());
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <E extends Section> void registerSection(Class<E> sectionClass, String ... patterns) throws IllegalArgumentException {
        Skript.checkAcceptRegistrations();
        skript.syntaxRegistry().register(SyntaxRegistry.SECTION, SyntaxInfo.builder(sectionClass).origin(Skript.getSyntaxOrigin(sectionClass)).addPatterns(patterns).build());
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static @Unmodifiable Collection<SyntaxElementInfo<? extends Statement>> getStatements() {
        return Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.STATEMENT).stream().map(SyntaxElementInfo::fromModern).collect(Collectors.toUnmodifiableList());
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static @Unmodifiable Collection<SyntaxElementInfo<? extends Condition>> getConditions() {
        return Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.CONDITION).stream().map(SyntaxElementInfo::fromModern).collect(Collectors.toUnmodifiableList());
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static @Unmodifiable Collection<SyntaxElementInfo<? extends Effect>> getEffects() {
        return Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.EFFECT).stream().map(SyntaxElementInfo::fromModern).collect(Collectors.toUnmodifiableList());
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static @Unmodifiable Collection<SyntaxElementInfo<? extends Section>> getSections() {
        return Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.SECTION).stream().map(SyntaxElementInfo::fromModern).collect(Collectors.toUnmodifiableList());
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <E extends Expression<T>, T> void registerExpression(Class<E> expressionClass, Class<T> returnType, ExpressionType type, String ... patterns) throws IllegalArgumentException {
        Skript.checkAcceptRegistrations();
        skript.syntaxRegistry().register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(expressionClass, returnType).priority(type.priority())).origin(Skript.getSyntaxOrigin(expressionClass))).addPatterns(patterns)).build());
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static Iterator<ExpressionInfo<?, ?>> getExpressions() {
        ArrayList<ExpressionInfo> list = new ArrayList<ExpressionInfo>();
        for (DefaultSyntaxInfos.Expression<?, ?> info : Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.EXPRESSION)) {
            list.add((ExpressionInfo)SyntaxElementInfo.fromModern(info));
        }
        return list.iterator();
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static Iterator<ExpressionInfo<?, ?>> getExpressions(Class<?> ... returnTypes) {
        return new CheckedIterator(Skript.getExpressions(), info -> {
            if (info == null || info.returnType == Object.class) {
                return true;
            }
            for (Class returnType : returnTypes) {
                assert (returnType != null);
                if (!Converters.converterExists(info.returnType, returnType)) continue;
                return true;
            }
            return false;
        });
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <E extends SkriptEvent> SkriptEventInfo<E> registerEvent(String name, Class<E> c, Class<? extends Event> event, String ... patterns) {
        return Skript.registerEvent(name, c, new Class[]{event}, patterns);
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <E extends SkriptEvent> SkriptEventInfo<E> registerEvent(String name, Class<E> eventClass, Class<? extends Event>[] events, String ... patterns) {
        Skript.checkAcceptRegistrations();
        for (int i = 0; i < patterns.length; ++i) {
            patterns[i] = BukkitSyntaxInfos.fixPattern(patterns[i]);
        }
        SkriptEventInfo.ModernSkriptEventInfo<E> legacy = new SkriptEventInfo.ModernSkriptEventInfo<E>(name, patterns, eventClass, "", events);
        skript.syntaxRegistry().register(BukkitSyntaxInfos.Event.KEY, legacy);
        return legacy;
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <E extends Structure> void registerStructure(Class<E> structureClass, String ... patterns) {
        Skript.checkAcceptRegistrations();
        skript.syntaxRegistry().register(SyntaxRegistry.STRUCTURE, ((DefaultSyntaxInfos.Structure.Builder)((DefaultSyntaxInfos.Structure.Builder)DefaultSyntaxInfos.Structure.builder(structureClass).origin(Skript.getSyntaxOrigin(structureClass))).addPatterns(patterns)).build());
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <E extends Structure> void registerSimpleStructure(Class<E> structureClass, String ... patterns) {
        Skript.checkAcceptRegistrations();
        skript.syntaxRegistry().register(SyntaxRegistry.STRUCTURE, ((DefaultSyntaxInfos.Structure.Builder)((DefaultSyntaxInfos.Structure.Builder)DefaultSyntaxInfos.Structure.builder(structureClass).origin(Skript.getSyntaxOrigin(structureClass))).addPatterns(patterns)).nodeType(DefaultSyntaxInfos.Structure.NodeType.SIMPLE).build());
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <E extends Structure> void registerStructure(Class<E> structureClass, EntryValidator entryValidator, String ... patterns) {
        Skript.registerStructure(structureClass, entryValidator, DefaultSyntaxInfos.Structure.NodeType.SECTION, patterns);
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <E extends Structure> void registerStructure(Class<E> structureClass, EntryValidator entryValidator, DefaultSyntaxInfos.Structure.NodeType nodeType, String ... patterns) {
        Skript.checkAcceptRegistrations();
        skript.syntaxRegistry().register(SyntaxRegistry.STRUCTURE, ((DefaultSyntaxInfos.Structure.Builder)((DefaultSyntaxInfos.Structure.Builder)DefaultSyntaxInfos.Structure.builder(structureClass).origin(Skript.getSyntaxOrigin(structureClass))).addPatterns(patterns)).entryValidator(entryValidator).nodeType(nodeType).build());
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static @Unmodifiable Collection<SkriptEventInfo<?>> getEvents() {
        return Skript.instance().syntaxRegistry().syntaxes(BukkitSyntaxInfos.Event.KEY).stream().map(SyntaxElementInfo::fromModern).collect(Collectors.toUnmodifiableList());
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static @Unmodifiable List<StructureInfo<? extends Structure>> getStructures() {
        return Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.STRUCTURE).stream().map(SyntaxElementInfo::fromModern).collect(Collectors.toUnmodifiableList());
    }

    public static boolean dispatchCommand(CommandSender sender, String command) {
        try {
            if (sender instanceof Player) {
                PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent((Player)sender, "/" + command);
                Bukkit.getPluginManager().callEvent((Event)e);
                if (e.isCancelled() || !e.getMessage().startsWith("/")) {
                    return false;
                }
                return Bukkit.dispatchCommand((CommandSender)e.getPlayer(), (String)e.getMessage().substring(1));
            }
            ServerCommandEvent e = new ServerCommandEvent(sender, command);
            Bukkit.getPluginManager().callEvent((Event)e);
            if (e.getCommand().isEmpty() || e.isCancelled()) {
                return false;
            }
            return Bukkit.dispatchCommand((CommandSender)e.getSender(), (String)e.getCommand());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean logNormal() {
        return SkriptLogger.log(Verbosity.NORMAL);
    }

    public static boolean logHigh() {
        return SkriptLogger.log(Verbosity.HIGH);
    }

    public static boolean logVeryHigh() {
        return SkriptLogger.log(Verbosity.VERY_HIGH);
    }

    public static boolean debug() {
        return SkriptLogger.debug();
    }

    public static boolean testing() {
        return Skript.debug() || Skript.class.desiredAssertionStatus();
    }

    public static boolean log(Verbosity minVerb) {
        return SkriptLogger.log(minVerb);
    }

    public static void debug(String info) {
        if (!Skript.debug()) {
            return;
        }
        SkriptLogger.log(SkriptLogger.DEBUG, info);
    }

    public static void debug(String message, Object ... objects) {
        if (!Skript.debug()) {
            return;
        }
        Skript.debug(message.formatted(objects));
    }

    public static void info(String info) {
        SkriptLogger.log(Level.INFO, info);
    }

    public static void warning(String warning) {
        SkriptLogger.log(Level.WARNING, warning);
    }

    public static void error(@Nullable String error) {
        if (error != null) {
            SkriptLogger.log(Level.SEVERE, error);
        }
    }

    public static void error(String message, Object ... objects) {
        Skript.error(message.formatted(objects));
    }

    public static void error(String error, ErrorQuality quality) {
        SkriptLogger.log(new LogEntry(SkriptLogger.SEVERE, quality, error));
    }

    public static EmptyStacktraceException exception(String ... info) {
        return Skript.exception(null, info);
    }

    public static EmptyStacktraceException exception(@Nullable Throwable cause, String ... info) {
        return Skript.exception(cause, null, null, info);
    }

    public static EmptyStacktraceException exception(@Nullable Throwable cause, @Nullable Thread thread, String ... info) {
        return Skript.exception(cause, thread, null, info);
    }

    public static EmptyStacktraceException exception(@Nullable Throwable cause, @Nullable TriggerItem item, String ... info) {
        return Skript.exception(cause, null, item, info);
    }

    public static void markErrored() {
        errored = true;
    }

    public static EmptyStacktraceException exception(@Nullable Throwable cause, @Nullable Thread thread, @Nullable TriggerItem item, String ... info) {
        errored = true;
        if (cause instanceof EmptyStacktraceException) {
            return new EmptyStacktraceException();
        }
        if (!checkedPlugins) {
            Skript.initializePluginPackages();
            checkedPlugins = true;
        }
        Skript.logErrorDetails(cause, info, thread, item);
        return new EmptyStacktraceException();
    }

    private static void initializePluginPackages() {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            PluginDescriptionFile desc;
            if (plugin.getName().equals("Skript") || !(desc = plugin.getDescription()).getDepend().contains("Skript") && !desc.getSoftDepend().contains("Skript")) continue;
            String mainClassPackage = Skript.getPackageName(desc.getMain());
            pluginPackages.put(mainClassPackage, desc);
            if (!Skript.debug()) continue;
            Skript.info("Identified potential addon: " + desc.getFullName() + " (" + mainClassPackage + ")");
        }
    }

    private static String getPackageName(String qualifiedClassName) {
        int lastDotIndex = qualifiedClassName.lastIndexOf(46);
        return lastDotIndex == -1 ? "" : qualifiedClassName.substring(0, lastDotIndex);
    }

    private static void logErrorDetails(@Nullable Throwable cause, String[] info, @Nullable Thread thread, @Nullable TriggerItem item) {
        String issuesUrl = "https://github.com/SkriptLang/Skript/issues";
        String downloadUrl = "https://github.com/SkriptLang/Skript/releases/latest";
        Skript.logEx();
        Skript.logEx("[Skript] Severe Error:");
        Skript.logEx(info);
        Skript.logEx();
        Set<PluginDescriptionFile> stackPlugins = Skript.identifyPluginsInStackTrace(Thread.currentThread().getStackTrace());
        Skript.logPlatformSupportInfo(issuesUrl, downloadUrl, stackPlugins);
        Skript.logEx();
        Skript.logEx("Stack trace:");
        Skript.logStackTrace(cause);
        Skript.logEx();
        Skript.logVersionInfo();
        Skript.logEx();
        Skript.logCurrentState(thread, item);
        Skript.logEx("End of Error.");
        Skript.logEx();
    }

    private static Set<PluginDescriptionFile> identifyPluginsInStackTrace(StackTraceElement[] stackTrace) {
        HashSet<PluginDescriptionFile> stackPlugins = new HashSet<PluginDescriptionFile>();
        for (StackTraceElement element : stackTrace) {
            pluginPackages.entrySet().stream().filter(entry -> element.getClassName().startsWith((String)entry.getKey())).forEach(entry -> stackPlugins.add((PluginDescriptionFile)entry.getValue()));
        }
        return stackPlugins;
    }

    private static void logPlatformSupportInfo(String issuesUrl, String downloadUrl, Set<PluginDescriptionFile> stackPlugins) {
        SkriptUpdater updater = Skript.getInstance().getUpdater();
        if (tainted) {
            Skript.logEx("Skript is running with developer command-line options. Consider disabling them if not a developer.");
        } else if (Skript.getInstance().getDescription().getVersion().contains("nightly")) {
            Skript.logEx("You're running a (buggy) nightly version of Skript. If this is not a test server, switch to a stable release.");
            Skript.logEx("Please report this bug to: " + issuesUrl);
        } else if (!Skript.serverPlatform.supported) {
            String supportedPlatforms = Skript.getSupportedPlatforms();
            Skript.logEx("Your server platform appears to be unsupported by Skript. Consider switching to one of the supported platforms (" + supportedPlatforms + ") for better compatibility.");
        } else if (updater != null && updater.getReleaseStatus() == ReleaseStatus.OUTDATED) {
            Skript.logEx("You're running an outdated version of Skript! Update to the latest version here: " + downloadUrl);
        } else {
            Skript.logEx("An unexpected error occurred with Skript. This issue is likely not your fault.");
            Skript.logExAddonInfo(issuesUrl, stackPlugins);
        }
    }

    private static String getSupportedPlatforms() {
        return Arrays.stream(ServerPlatform.values()).filter(platform -> platform.supported).map(Enum::name).collect(Collectors.joining(", "));
    }

    private static void logExAddonInfo(String issuesUrl, Set<PluginDescriptionFile> stackPlugins) {
        if (pluginPackages.isEmpty()) {
            Skript.logEx("Report the issue: " + issuesUrl);
        } else {
            Skript.logEx("You are using some plugins that alter how Skript works (addons).");
            if (stackPlugins.isEmpty()) {
                Skript.logEx("Full list of addons:");
                pluginPackages.values().forEach(desc -> Skript.logEx(Skript.getPluginDescription(desc)));
                Skript.logEx("We could not identify related addons, it might also be a Skript issue.");
            } else {
                Skript.logEx("The following plugins are likely related to this error:");
                stackPlugins.forEach(desc -> Skript.logEx(Skript.getPluginDescription(desc)));
            }
            Skript.logEx("Try temporarily removing the listed plugins one by one to identify the cause.");
            Skript.logEx("If removing a plugin resolves the issue, please report the problem to the plugin developer.");
        }
    }

    private static String getPluginDescription(PluginDescriptionFile desc) {
        String website = desc.getWebsite();
        return desc.getFullName() + (String)(website != null && !website.isEmpty() ? " (" + website + ")" : "");
    }

    private static void logStackTrace(@Nullable Throwable cause) {
        if (cause == null || cause.getStackTrace().length == 0) {
            Skript.logEx("Warning: no/empty exception given, dumping current stack trace instead");
            cause = new Exception("EmptyStacktraceException cause");
        }
        while (cause != null) {
            Skript.logEx((cause == null ? "" : "Caused by: ") + cause.toString());
            for (StackTraceElement element : cause.getStackTrace()) {
                Skript.logEx("    at " + element.toString());
            }
            cause = cause.getCause();
        }
    }

    private static void logVersionInfo() {
        SkriptUpdater updater = Skript.getInstance().getUpdater();
        if (updater != null) {
            ReleaseStatus status = updater.getReleaseStatus();
            Skript.logEx("Skript: " + String.valueOf(Skript.getVersion()) + " (" + status.toString() + ")");
            ReleaseManifest current = updater.getCurrentRelease();
            Skript.logEx("    Flavor: " + current.flavor);
            Skript.logEx("    Date: " + current.date);
        } else {
            Skript.logEx("Skript: " + String.valueOf(Skript.getVersion()) + " (unknown; likely custom)");
        }
        Skript.logEx("Bukkit: " + Bukkit.getBukkitVersion());
        Skript.logEx("Minecraft: " + String.valueOf(Skript.getMinecraftVersion()));
        Skript.logEx("Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + ")");
        Skript.logEx("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version"));
        Skript.logEx();
        Skript.logEx("Server platform: " + Skript.serverPlatform.name + (Skript.serverPlatform.supported ? "" : " (unsupported)"));
    }

    private static void logCurrentState(@Nullable Thread thread, @Nullable TriggerItem item) {
        Skript.logEx("Current node: " + String.valueOf(SkriptLogger.getNode()));
        Skript.logEx("Current item: " + (item == null ? "null" : item.toString(null, true)));
        if (item != null && item.getTrigger() != null) {
            Trigger trigger = item.getTrigger();
            Script script = trigger.getScript();
            Skript.logEx("Current trigger: " + trigger.toString(null, true) + " (" + (script == null ? "null" : script.getConfig().getFileName()) + ", line " + trigger.getLineNumber() + ")");
        }
        Skript.logEx("Thread: " + (thread == null ? Thread.currentThread() : thread).getName());
        Skript.logEx("Language: " + Language.getName());
        Skript.logEx("Link parse mode: " + String.valueOf((Object)TextComponentParser.instance().linkParseMode()));
    }

    static void logEx() {
        SkriptLogger.LOGGER.severe(EXCEPTION_PREFIX);
    }

    static void logEx(String ... lines) {
        for (String line : lines) {
            SkriptLogger.LOGGER.severe(EXCEPTION_PREFIX + line);
        }
    }

    public static String getSkriptPrefix() {
        return SKRIPT_PREFIX_MESSAGE.getValueOrDefault("<grey>[<gold>Skript<grey>] <reset>");
    }

    public static void info(CommandSender sender, String info) {
        sender.sendMessage(TextComponentParser.instance().parseSafe(Skript.getSkriptPrefix() + info));
    }

    public static void broadcast(String message, String permission) {
        Bukkit.broadcast((Component)TextComponentParser.instance().parseSafe(Skript.getSkriptPrefix() + message), (String)permission);
    }

    public static void adminBroadcast(String message) {
        Skript.broadcast(message, "skript.admin");
    }

    public static void message(CommandSender sender, String info) {
        sender.sendMessage(TextComponentParser.instance().parseSafe(info));
    }

    public static void error(CommandSender sender, String error) {
        sender.sendMessage(TextComponentParser.instance().parseSafe(Skript.getSkriptPrefix() + "<dark_red>" + error));
    }

    @Nullable
    public SkriptUpdater getUpdater() {
        return this.updater;
    }

    static {
        m_invalid_reload = new Message("skript.invalid reload");
        m_finished_loading = new Message("skript.finished loading");
        m_no_errors = new Message("skript.no errors");
        m_no_scripts = new Message("skript.no scripts");
        m_scripts_loaded = new PluralizingArgsMessage("skript.scripts loaded");
        WARNING_MESSAGE = new Message("skript.warning message");
        RESTART_MESSAGE = new Message("skript.restart message");
        disabledHookRegistrations = new HashSet();
        finishedLoadingHooks = false;
        closeOnDisable = Collections.synchronizedCollection(new ArrayList());
        IS_STOPPING_EXISTS = Skript.methodExists(Server.class, "isStopping", new Class[0]);
        if (!IS_STOPPING_EXISTS) {
            Method serverMethod;
            Server server = Bukkit.getServer();
            Class clazz = server.getClass();
            try {
                serverMethod = clazz.getMethod("getServer", new Class[0]);
            }
            catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            try {
                MC_SERVER = serverMethod.invoke((Object)server, new Object[0]);
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            try {
                String isRunningMethod = "isRunning";
                if (Skript.isRunningMinecraft(1, 20, 5)) {
                    isRunningMethod = "x";
                } else if (Skript.isRunningMinecraft(1, 20)) {
                    isRunningMethod = "v";
                } else if (Skript.isRunningMinecraft(1, 19)) {
                    isRunningMethod = "u";
                } else if (Skript.isRunningMinecraft(1, 18)) {
                    isRunningMethod = "v";
                }
                IS_RUNNING = MC_SERVER.getClass().getMethod(isRunningMethod, new Class[0]);
            }
            catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        UEH = new Thread.UncaughtExceptionHandler(){

            @Override
            public void uncaughtException(@Nullable Thread t, @Nullable Throwable e) {
                Skript.exception(e, "Exception in thread " + (t == null ? null : t.getName()));
            }
        };
        acceptRegistrations = true;
        addons = new HashSet<SkriptAddon>();
        pluginPackages = new HashMap<String, PluginDescriptionFile>();
        checkedPlugins = false;
        tainted = false;
        errored = false;
        SKRIPT_PREFIX_MESSAGE = new Message("skript.prefix");
    }

    private class JoinUpdateNotificationListener
    implements Listener {
        private JoinUpdateNotificationListener() {
        }

        @EventHandler
        public void onJoin(final PlayerJoinEvent event) {
            if (!event.getPlayer().hasPermission("skript.admin")) {
                return;
            }
            new Task(this, (Plugin)Skript.this, 0L){
                final /* synthetic */ JoinUpdateNotificationListener this$1;
                {
                    this.this$1 = this$1;
                    super(plugin, delay);
                }

                @Override
                public void run() {
                    Player player = event.getPlayer();
                    SkriptUpdater updater = this.this$1.Skript.this.getUpdater();
                    if (updater == null || updater.getReleaseStatus() != ReleaseStatus.OUTDATED) {
                        return;
                    }
                    UpdateManifest update = updater.getUpdateManifest();
                    if (update == null) {
                        return;
                    }
                    Skript.info((CommandSender)player, SkriptUpdater.m_update_available.toString(update.id, Skript.getVersion()));
                    player.sendMessage(TextComponentParser.instance().parse("Download it at: <aqua><underlined><click:open_url:" + String.valueOf(update.downloadUrl) + ">" + String.valueOf(update.downloadUrl)));
                }
            };
        }
    }

    private static class ServerReloadListener
    implements Listener {
        private ServerReloadListener() {
        }

        @EventHandler
        public void onServerReload(ServerLoadEvent event) {
            if (event.getType() != ServerLoadEvent.LoadType.RELOAD) {
                return;
            }
            for (OfflinePlayer player : Bukkit.getOperators()) {
                if (!player.isOnline()) continue;
                player.getPlayer().sendMessage(String.valueOf(ChatColor.YELLOW) + Skript.getWarningMessage());
                player.getPlayer().sendMessage(String.valueOf(ChatColor.YELLOW) + Skript.getRestartMessage());
            }
            Skript.warning(Skript.getWarningMessage());
            Skript.warning(Skript.getRestartMessage());
        }
    }
}

