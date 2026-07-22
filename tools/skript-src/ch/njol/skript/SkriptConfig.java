/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  co.aikar.timings.Timings
 *  org.bukkit.event.EventPriority
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptUpdater;
import ch.njol.skript.classes.EnumParser;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Option;
import ch.njol.skript.config.OptionSection;
import ch.njol.skript.hooks.Hook;
import ch.njol.skript.hooks.VaultHook;
import ch.njol.skript.hooks.regions.GriefPreventionHook;
import ch.njol.skript.hooks.regions.PreciousStonesHook;
import ch.njol.skript.hooks.regions.ResidenceHook;
import ch.njol.skript.hooks.regions.WorldGuardHook;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.localization.Language;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.log.Verbosity;
import ch.njol.skript.timings.SkriptTimings;
import ch.njol.skript.update.ReleaseChannel;
import ch.njol.skript.util.FileUtils;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.util.Version;
import ch.njol.skript.variables.FlatFileStorage;
import ch.njol.skript.variables.Variables;
import co.aikar.timings.Timings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentParser;
import org.skriptlang.skript.util.event.EventRegistry;

public class SkriptConfig {
    private static final EventRegistry<Event> eventRegistry = new EventRegistry();
    @Nullable
    static Config mainConfig;
    static Collection<Config> configs;
    static final Option<String> version;
    public static final Option<String> language;
    public static final Option<Boolean> checkForNewVersion;
    public static final Option<Timespan> updateCheckInterval;
    static final Option<Integer> updaterDownloadTries;
    public static final Option<String> releaseChannel;
    public static final Option<Boolean> enableEffectCommands;
    public static final Option<String> effectCommandToken;
    public static final Option<Boolean> allowOpsToUseEffectCommands;
    public static final Option<Boolean> logEffectCommands;
    public static final Option<Boolean> compressBackups;
    public static final OptionSection databases;
    public static final Option<Boolean> usePlayerUUIDsInVariableNames;
    public static final Option<Boolean> enablePlayerVariableFix;
    private static final DateFormat shortDateFormat;
    public static final Option<DateFormat> dateFormat;
    public static final Option<Verbosity> verbosity;
    public static final Option<EventPriority> defaultEventPriority;
    public static final Option<Boolean> listenCancelledByDefault;
    public static final Option<Integer> numberAccuracy;
    public static final Option<Integer> maxTargetBlockDistance;
    public static final Option<Boolean> caseSensitive;
    public static final Option<Boolean> allowFunctionsBeforeDefs;
    public static final Option<Boolean> disableObjectCannotBeSavedWarnings;
    public static final Option<Boolean> disableMissingAndOrWarnings;
    public static final Option<Boolean> disableVariableStartingWithExpressionWarnings;
    public static final Option<Boolean> disableColonInVariableWarnings;
    public static final Option<Boolean> disableUnreachableCodeWarnings;
    @Deprecated(since="2.3.0", forRemoval=true)
    public static final Option<Boolean> enableScriptCaching;
    public static final Option<Boolean> keepConfigsLoaded;
    public static final Option<Boolean> addonSafetyChecks;
    public static final Option<Boolean> apiSoftExceptions;
    public static final Option<Boolean> enableTimings;
    public static final Option<Boolean> caseInsensitiveVariables;
    public static final Option<Boolean> caseInsensitiveCommands;
    public static final Option<String[]> safeTags;
    public static final Option<String> parseLinks;
    public static final Option<Boolean> colorResetCodes;
    public static final Option<String> scriptLoaderThreadSize;
    public static final Option<Boolean> useTypeProperties;
    public static final Option<Boolean> allowUnsafePlatforms;
    public static final Option<Boolean> keepLastUsageDates;
    public static final Option<Boolean> loadDefaultAliases;
    public static final Option<Boolean> executeFunctionsWithMissingParams;
    public static final Option<Boolean> disableHookVault;
    public static final Option<Boolean> disableHookGriefPrevention;
    public static final Option<Boolean> disableHookPreciousStones;
    public static final Option<Boolean> disableHookResidence;
    public static final Option<Boolean> disableHookWorldGuard;
    public static final Option<Pattern> playerNameRegexPattern;
    public static final Option<Timespan> longParseTimeWarningThreshold;
    public static final Option<Timespan> runtimeErrorFrameDuration;
    public static final Option<Integer> runtimeErrorLimitTotal;
    public static final Option<Integer> runtimeWarningLimitTotal;
    public static final Option<Integer> runtimeErrorLimitLine;
    public static final Option<Integer> runtimeWarningLimitLine;
    public static final Option<Integer> runtimeErrorLimitLineTimeout;
    public static final Option<Integer> runtimeWarningLimitLineTimeout;
    public static final Option<Integer> runtimeErrorTimeoutDuration;
    public static final Option<Integer> runtimeWarningTimeoutDuration;
    public static final Option<Integer> variableChangesUntilSave;
    public static final Option<Boolean> simplifySyntaxesOnParse;

    public static EventRegistry<Event> eventRegistry() {
        return eventRegistry;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static String formatDate(long timestamp) {
        DateFormat f;
        DateFormat dateFormat = f = SkriptConfig.dateFormat.value();
        synchronized (dateFormat) {
            return f.format(timestamp);
        }
    }

    private static void userDisableHooks(Class<? extends Hook<?>> hookClass, boolean value) {
        if (Skript.isFinishedLoadingHooks()) {
            Skript.error("Hooks cannot be disabled once the server has started. Please restart the server to disable the hooks.");
            return;
        }
        if (value) {
            Skript.disableHookRegistration(hookClass);
        }
    }

    @Nullable
    public static Config getConfig() {
        return mainConfig;
    }

    static void load() {
        if (mainConfig != null) {
            mainConfig.invalidate();
        }
        try {
            Config mainConfig;
            File configFile = new File(Skript.getInstance().getDataFolder(), "config.sk");
            if (!configFile.exists()) {
                Skript.error("Config file 'config.sk' does not exist!");
                return;
            }
            if (!configFile.canRead()) {
                Skript.error("Config file 'config.sk' cannot be read!");
                return;
            }
            try {
                mainConfig = new Config(configFile, false, false, ":");
            }
            catch (IOException ex) {
                Skript.exception((Throwable)ex, "Could not load the main config");
                return;
            }
            SkriptConfig.mainConfig = mainConfig;
            String configVersion = mainConfig.getValue(SkriptConfig.version.key);
            if (configVersion == null || Skript.getVersion().compareTo(new Version(configVersion)) != 0) {
                if (!mainConfig.getMainNode().isValid()) {
                    Skript.error("Your config is outdated, but cannot be updated because it contains errors.");
                    return;
                }
                try (InputStream stream = Skript.getInstance().getResource("config.sk");){
                    if (stream == null) {
                        Skript.error("Your config is outdated, but Skript couldn't find the newest config in its jar.");
                        return;
                    }
                    Config newConfig = new Config(stream, "Skript.jar/config.sk", false, false, ":");
                    File backup = FileUtils.backup(configFile);
                    boolean updated = mainConfig.updateNodes(newConfig);
                    mainConfig.getMainNode().set(SkriptConfig.version.key, Skript.getVersion().toString());
                    mainConfig.save(configFile);
                    SkriptConfig.mainConfig = mainConfig;
                    if (updated) {
                        Skript.info("Your configuration has been updated to the latest version. A backup of your old config file has been created as " + backup.getName());
                    } else {
                        Skript.info("Your configuration is outdated, but no changes were performed. A backup of your config file has been created as " + backup.getName());
                    }
                }
                catch (IOException ex) {
                    Skript.exception((Throwable)ex, "Could not update the main config");
                    return;
                }
            }
            mainConfig.load(SkriptConfig.class);
        }
        catch (RuntimeException ex) {
            Skript.exception((Throwable)ex, "An error occurred while loading the config");
        }
        SkriptConfig.eventRegistry().events(ReloadEvent.class).forEach(ReloadEvent::onReload);
    }

    static {
        configs = new ArrayList<Config>();
        version = new Option<String>("version", Skript.getVersion().toString()).optional(true);
        language = new Option<String>("language", "english").optional(true).setter(s -> {
            if (!Language.load(s)) {
                Skript.error("No language file found for '" + s + "'!");
            }
        });
        checkForNewVersion = new Option<Boolean>("check for new version", false).setter(t -> {
            SkriptUpdater updater = Skript.getInstance().getUpdater();
            if (updater != null) {
                updater.setEnabled((boolean)t);
            }
        });
        updateCheckInterval = new Option<Timespan>("update check interval", new Timespan(43200000L)).setter(t -> {
            SkriptUpdater updater = Skript.getInstance().getUpdater();
            if (updater != null) {
                updater.setCheckFrequency(t.getAs(Timespan.TimePeriod.TICK));
            }
        });
        updaterDownloadTries = new Option<Integer>("updater download tries", 7).optional(true);
        releaseChannel = new Option<String>("release channel", "none").setter(t -> {
            ReleaseChannel channel;
            switch (t) {
                case "alpha": 
                case "beta": {
                    Skript.warning("'alpha' and 'beta' are no longer valid release channels. Use 'prerelease' instead.");
                }
                case "prerelease": {
                    channel = new ReleaseChannel(name -> true, (String)t);
                    break;
                }
                case "stable": {
                    channel = new ReleaseChannel(name -> !name.contains("-"), (String)t);
                    break;
                }
                case "none": {
                    channel = new ReleaseChannel(name -> false, (String)t);
                    break;
                }
                default: {
                    channel = new ReleaseChannel(name -> false, (String)t);
                    Skript.error("Unknown release channel '" + t + "'.");
                }
            }
            SkriptUpdater updater = Skript.getInstance().getUpdater();
            if (updater != null) {
                updater.setReleaseChannel(channel);
            }
        });
        enableEffectCommands = new Option<Boolean>("enable effect commands", false);
        effectCommandToken = new Option<String>("effect command token", "!");
        allowOpsToUseEffectCommands = new Option<Boolean>("allow ops to use effect commands", false);
        logEffectCommands = new Option<Boolean>("log effect commands", false);
        compressBackups = new Option<Boolean>("compress backups", false);
        databases = new OptionSection("databases");
        usePlayerUUIDsInVariableNames = new Option<Boolean>("use player UUIDs in variable names", false);
        enablePlayerVariableFix = new Option<Boolean>("player variable fix", true);
        shortDateFormat = DateFormat.getDateTimeInstance(3, 3);
        dateFormat = new Option<DateFormat>("date format", shortDateFormat, s -> {
            try {
                if (s.equalsIgnoreCase("default")) {
                    return null;
                }
                return new SimpleDateFormat((String)s);
            }
            catch (IllegalArgumentException e) {
                Skript.error("'" + s + "' is not a valid date format. Please refer to https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html for instructions on the format.");
                return null;
            }
        });
        verbosity = new Option<Verbosity>("verbosity", Verbosity.NORMAL, new EnumParser<Verbosity>(Verbosity.class, "verbosity")).setter(SkriptLogger::setVerbosity);
        defaultEventPriority = new Option<EventPriority>("plugin priority", EventPriority.NORMAL, s -> {
            try {
                return EventPriority.valueOf((String)s.toUpperCase(Locale.ENGLISH));
            }
            catch (IllegalArgumentException e) {
                Skript.error("The plugin priority has to be one of lowest, low, normal, high, or highest.");
                return null;
            }
        });
        listenCancelledByDefault = new Option<Boolean>("listen to cancelled events by default", false).optional(true);
        numberAccuracy = new Option<Integer>("number accuracy", 2);
        maxTargetBlockDistance = new Option<Integer>("maximum target block distance", 100);
        caseSensitive = new Option<Boolean>("case sensitive", false);
        allowFunctionsBeforeDefs = new Option<Boolean>("allow function calls before definations", false).optional(true);
        disableObjectCannotBeSavedWarnings = new Option<Boolean>("disable variable will not be saved warnings", false);
        disableMissingAndOrWarnings = new Option<Boolean>("disable variable missing and/or warnings", false);
        disableVariableStartingWithExpressionWarnings = new Option<Boolean>("disable starting a variable's name with an expression warnings", false);
        disableColonInVariableWarnings = new Option<Boolean>("disable single colon in variable name warnings", false);
        disableUnreachableCodeWarnings = new Option<Boolean>("disable unreachable code warnings", false);
        enableScriptCaching = new Option<Boolean>("enable script caching", false).optional(true);
        keepConfigsLoaded = new Option<Boolean>("keep configs loaded", false).optional(true);
        addonSafetyChecks = new Option<Boolean>("addon safety checks", false).optional(true);
        apiSoftExceptions = new Option<Boolean>("soft api exceptions", false);
        enableTimings = new Option<Boolean>("enable timings", false).setter(t -> {
            if (!Skript.classExists("co.aikar.timings.Timings")) {
                if (t.booleanValue()) {
                    Skript.warning("Timings cannot be enabled! You are running Bukkit/Spigot, but Paper is required.");
                }
                SkriptTimings.setEnabled(false);
                return;
            }
            if (Timings.class.isAnnotationPresent(Deprecated.class)) {
                if (t.booleanValue()) {
                    Skript.warning("Timings cannot be enabled! Paper no longer supports Timings as of 1.19.4.");
                }
                SkriptTimings.setEnabled(false);
                return;
            }
            if (t.booleanValue()) {
                Skript.info("Timings support enabled!");
            }
            SkriptTimings.setEnabled(t);
        });
        caseInsensitiveVariables = new Option<Boolean>("case-insensitive variables", true).setter(t -> {
            Variables.caseInsensitiveVariables = t;
        });
        caseInsensitiveCommands = new Option<Boolean>("case-insensitive commands", false).optional(true);
        safeTags = new Option<String[]>("safe tags", new String[]{"color", "decorations", "gradient", "rainbow", "reset", "transition", "pride", "shadowColor"}, raw -> raw.split(", ")).setter(tags -> {
            try {
                TextComponentParser.instance().setSafeTags((String)tags);
            }
            catch (Error error) {
                // empty catch block
            }
        });
        parseLinks = new Option<String>("parse links in chat messages", "disabled").setter(mode -> {
            try {
                TextComponentParser textComponentParser = TextComponentParser.instance();
                textComponentParser.linkParseMode(switch (mode) {
                    case "false", "disabled" -> TextComponentParser.LinkParseMode.DISABLED;
                    case "true", "lenient" -> TextComponentParser.LinkParseMode.LENIENT;
                    case "strict" -> TextComponentParser.LinkParseMode.STRICT;
                    default -> {
                        Skript.warning("Unknown link parse mode: " + mode + ", please use disabled, strict or lenient");
                        yield TextComponentParser.LinkParseMode.DISABLED;
                    }
                });
            }
            catch (Error error) {
                // empty catch block
            }
        });
        colorResetCodes = new Option<Boolean>("color codes reset formatting", true).setter(causeReset -> {
            try {
                TextComponentParser.instance().colorsCauseReset((boolean)causeReset);
            }
            catch (Error error) {
                // empty catch block
            }
        });
        scriptLoaderThreadSize = new Option<String>("script loader thread size", "0").setter(s -> {
            int asyncLoaderSize;
            if (s.equalsIgnoreCase("processor count")) {
                asyncLoaderSize = Runtime.getRuntime().availableProcessors();
            } else {
                try {
                    asyncLoaderSize = Integer.parseInt(s);
                }
                catch (NumberFormatException e) {
                    Skript.error("Invalid option: " + s);
                    return;
                }
            }
            ScriptLoader.setAsyncLoaderSize(asyncLoaderSize);
        }).optional(true);
        useTypeProperties = new Option<Boolean>("use type properties", true).optional(false);
        allowUnsafePlatforms = new Option<Boolean>("allow unsafe platforms", false).optional(true);
        keepLastUsageDates = new Option<Boolean>("keep command last usage dates", false).optional(true);
        loadDefaultAliases = new Option<Boolean>("load default aliases", true).optional(true);
        executeFunctionsWithMissingParams = new Option<Boolean>("execute functions with missing parameters", true).optional(true).setter(t -> {
            Function.executeWithNulls = t;
        });
        disableHookVault = new Option<Boolean>("disable hooks.vault", false).optional(true).setter(value -> SkriptConfig.userDisableHooks(VaultHook.class, value));
        disableHookGriefPrevention = new Option<Boolean>("disable hooks.regions.grief prevention", false).optional(true).setter(value -> SkriptConfig.userDisableHooks(GriefPreventionHook.class, value));
        disableHookPreciousStones = new Option<Boolean>("disable hooks.regions.precious stones", false).optional(true).setter(value -> SkriptConfig.userDisableHooks(PreciousStonesHook.class, value));
        disableHookResidence = new Option<Boolean>("disable hooks.regions.residence", false).optional(true).setter(value -> SkriptConfig.userDisableHooks(ResidenceHook.class, value));
        disableHookWorldGuard = new Option<Boolean>("disable hooks.regions.worldguard", false).optional(true).setter(value -> SkriptConfig.userDisableHooks(WorldGuardHook.class, value));
        playerNameRegexPattern = new Option<Pattern>("player name regex pattern", Pattern.compile("[a-zA-Z0-9_]{1,16}"), s -> {
            try {
                return Pattern.compile(s);
            }
            catch (PatternSyntaxException e) {
                Skript.error("Invalid player name regex pattern: " + e.getMessage());
                return null;
            }
        }).optional(true);
        longParseTimeWarningThreshold = new Option<Timespan>("long parse time warning threshold", new Timespan(0L));
        runtimeErrorFrameDuration = new Option<Timespan>("runtime errors.frame duration", new Timespan(Timespan.TimePeriod.SECOND, 1L));
        runtimeErrorLimitTotal = new Option<Integer>("runtime errors.total errors per frame", 8);
        runtimeWarningLimitTotal = new Option<Integer>("runtime errors.total warnings per frame", 8);
        runtimeErrorLimitLine = new Option<Integer>("runtime errors.errors from one line per frame", 2);
        runtimeWarningLimitLine = new Option<Integer>("runtime errors.warnings from one line per frame", 2);
        runtimeErrorLimitLineTimeout = new Option<Integer>("runtime errors.error spam timeout limit", 4);
        runtimeWarningLimitLineTimeout = new Option<Integer>("runtime errors.warning spam timeout limit", 4);
        runtimeErrorTimeoutDuration = new Option<Integer>("runtime errors.error timeout length", 10);
        runtimeWarningTimeoutDuration = new Option<Integer>("runtime errors.warning timeout length", 10);
        variableChangesUntilSave = new Option<Integer>("variable changes until save", 1000).setter(FlatFileStorage::setRequiredChangesForResave);
        simplifySyntaxesOnParse = new Option<Boolean>("simplify syntax on parse", true).optional(true);
    }

    @FunctionalInterface
    public static interface ReloadEvent
    extends Event {
        public void onReload();
    }

    public static interface Event
    extends org.skriptlang.skript.util.event.Event {
    }
}

