/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashBasedTable
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.SkriptUpdater;
import ch.njol.skript.bstats.bukkit.Metrics;
import ch.njol.skript.bstats.charts.DrilldownPie;
import ch.njol.skript.bstats.charts.SimplePie;
import ch.njol.skript.config.Option;
import ch.njol.skript.localization.Language;
import ch.njol.skript.util.Version;
import com.google.common.collect.HashBasedTable;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentParser;

public class SkriptMetrics {
    public static void setupMetrics(Metrics metrics) {
        SkriptMetrics.setupLegacyMetrics(metrics);
        metrics.addCustomChart(new DrilldownPie("drilldownPluginVersion", () -> {
            Version version = Skript.getVersion();
            HashBasedTable table = HashBasedTable.create((int)1, (int)1);
            table.put((Object)(version.getMajor() + "." + version.getMinor()), (Object)version.toString(), (Object)1);
            return table.rowMap();
        }));
        metrics.addCustomChart(new DrilldownPie("drilldownMinecraftVersion", () -> {
            Version version = Skript.getMinecraftVersion();
            HashBasedTable table = HashBasedTable.create((int)1, (int)1);
            table.put((Object)(version.getMajor() + "." + version.getMinor()), (Object)version.toString(), (Object)1);
            return table.rowMap();
        }));
        metrics.addCustomChart(new SimplePie("buildFlavor", () -> {
            SkriptUpdater updater = Skript.getInstance().getUpdater();
            if (updater != null) {
                return updater.getCurrentRelease().flavor;
            }
            return "unknown";
        }));
        metrics.addCustomChart(new DrilldownPie("drilldownPluginLanguage", () -> {
            String lang = Language.getName();
            return SkriptMetrics.isDefaultMap(lang, SkriptConfig.language.defaultValue());
        }));
        metrics.addCustomChart(new DrilldownPie("drilldownUpdateChecker", () -> {
            HashBasedTable table = HashBasedTable.create((int)1, (int)1);
            table.put((Object)SkriptConfig.checkForNewVersion.value().toString(), (Object)SkriptConfig.updateCheckInterval.value().toString(), (Object)1);
            return table.rowMap();
        }));
        metrics.addCustomChart(new SimplePie("releaseChannel", SkriptConfig.releaseChannel::value));
        metrics.addCustomChart(new DrilldownPie("drilldownEffectCommands", () -> {
            HashBasedTable table = HashBasedTable.create((int)1, (int)1);
            table.put((Object)SkriptConfig.enableEffectCommands.value().toString(), (Object)SkriptConfig.effectCommandToken.value(), (Object)1);
            return table.rowMap();
        }));
        metrics.addCustomChart(new SimplePie("effectCommandsOps", () -> SkriptConfig.allowOpsToUseEffectCommands.value().toString()));
        metrics.addCustomChart(new SimplePie("logEffectCommands", () -> SkriptConfig.logEffectCommands.value().toString()));
        metrics.addCustomChart(new SimplePie("loadDefaultAliases", () -> SkriptConfig.loadDefaultAliases.value().toString()));
        metrics.addCustomChart(new SimplePie("playerVariableFix", () -> SkriptConfig.enablePlayerVariableFix.value().toString()));
        metrics.addCustomChart(new SimplePie("uuidsWithPlayers", () -> SkriptConfig.usePlayerUUIDsInVariableNames.value().toString()));
        metrics.addCustomChart(new DrilldownPie("drilldownDateFormat", () -> {
            String value = ((SimpleDateFormat)SkriptConfig.dateFormat.value()).toPattern();
            String defaultValue = ((SimpleDateFormat)SkriptConfig.dateFormat.defaultValue()).toPattern();
            return SkriptMetrics.isDefaultMap(value, defaultValue, "default");
        }));
        metrics.addCustomChart(new DrilldownPie("drilldownLogVerbosity", () -> {
            String verbosity = SkriptConfig.verbosity.value().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
            String defaultValue = SkriptConfig.verbosity.defaultValue().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
            return SkriptMetrics.isDefaultMap(verbosity, defaultValue);
        }));
        metrics.addCustomChart(new DrilldownPie("drilldownPluginPriority", () -> {
            String priority = SkriptConfig.defaultEventPriority.value().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
            String defaultValue = SkriptConfig.defaultEventPriority.defaultValue().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
            return SkriptMetrics.isDefaultMap(priority, defaultValue);
        }));
        metrics.addCustomChart(new SimplePie("cancelledByDefault", () -> SkriptConfig.listenCancelledByDefault.value().toString()));
        metrics.addCustomChart(new DrilldownPie("drilldownNumberAccuracy", () -> SkriptMetrics.isDefaultMap(SkriptConfig.numberAccuracy)));
        metrics.addCustomChart(new DrilldownPie("drilldownMaxTargetDistance", () -> SkriptMetrics.isDefaultMap(SkriptConfig.maxTargetBlockDistance)));
        metrics.addCustomChart(new SimplePie("caseSensitiveFunctions", () -> SkriptConfig.caseSensitive.value().toString()));
        metrics.addCustomChart(new SimplePie("caseSensitiveVariables", () -> String.valueOf(SkriptConfig.caseInsensitiveVariables.value() == false)));
        metrics.addCustomChart(new SimplePie("caseSensitiveCommands", () -> String.valueOf(SkriptConfig.caseInsensitiveCommands.value() == false)));
        metrics.addCustomChart(new SimplePie("disableSaveWarnings", () -> SkriptConfig.disableObjectCannotBeSavedWarnings.value().toString()));
        metrics.addCustomChart(new SimplePie("disableAndOrWarnings", () -> SkriptConfig.disableMissingAndOrWarnings.value().toString()));
        metrics.addCustomChart(new SimplePie("disableStartsWithWarnings", () -> SkriptConfig.disableVariableStartingWithExpressionWarnings.value().toString()));
        metrics.addCustomChart(new SimplePie("softApiExceptions", () -> SkriptConfig.apiSoftExceptions.value().toString()));
        metrics.addCustomChart(new SimplePie("timingsStatus", () -> {
            if (!Skript.classExists("co.aikar.timings.Timings")) {
                return "unsupported";
            }
            return SkriptConfig.enableTimings.value().toString();
        }));
        metrics.addCustomChart(new SimplePie("parseLinks", () -> TextComponentParser.instance().linkParseMode().name().toLowerCase(Locale.ENGLISH)));
        metrics.addCustomChart(new SimplePie("colorResetCodes", () -> SkriptConfig.colorResetCodes.value().toString()));
        metrics.addCustomChart(new SimplePie("keepLastUsage", () -> SkriptConfig.keepLastUsageDates.value().toString()));
        metrics.addCustomChart(new DrilldownPie("drilldownParsetimeWarningThreshold", () -> SkriptMetrics.isDefaultMap(SkriptConfig.longParseTimeWarningThreshold, "disabled")));
    }

    private static void setupLegacyMetrics(Metrics metrics) {
        metrics.addCustomChart(new SimplePie("pluginLanguage", Language::getName));
        metrics.addCustomChart(new SimplePie("updateCheckerEnabled", () -> SkriptConfig.checkForNewVersion.value().toString()));
        metrics.addCustomChart(new SimplePie("logVerbosity", () -> SkriptConfig.verbosity.value().name().toLowerCase(Locale.ENGLISH).replace('_', ' ')));
        metrics.addCustomChart(new SimplePie("pluginPriority", () -> SkriptConfig.defaultEventPriority.value().name().toLowerCase(Locale.ENGLISH).replace('_', ' ')));
        metrics.addCustomChart(new SimplePie("effectCommands", () -> SkriptConfig.enableEffectCommands.value().toString()));
        metrics.addCustomChart(new SimplePie("maxTargetDistance", () -> SkriptConfig.maxTargetBlockDistance.value().toString()));
    }

    private static <T> Map<String, Map<String, Integer>> isDefaultMap(@Nullable T value, T defaultValue) {
        return SkriptMetrics.isDefaultMap(value, defaultValue, defaultValue.toString());
    }

    private static <T> Map<String, Map<String, Integer>> isDefaultMap(@Nullable T value, @Nullable T defaultValue, String defaultLabel) {
        HashBasedTable table = HashBasedTable.create((int)1, (int)1);
        table.put((Object)(Objects.equals(value, defaultValue) ? defaultLabel : "other"), (Object)String.valueOf(value), (Object)1);
        return table.rowMap();
    }

    private static <T> Map<String, Map<String, Integer>> isDefaultMap(Option<T> option) {
        return SkriptMetrics.isDefaultMap(option.value(), option.defaultValue(), option.defaultValue().toString());
    }

    private static <T> Map<String, Map<String, Integer>> isDefaultMap(Option<T> option, String defaultLabel) {
        return SkriptMetrics.isDefaultMap(option.value(), option.defaultValue(), defaultLabel);
    }
}

