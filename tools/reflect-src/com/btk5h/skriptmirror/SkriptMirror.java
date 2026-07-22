/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.SkriptAddon
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.registrations.Classes
 *  ch.njol.skript.util.Version
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.skriptlang.skript.lang.comparator.Comparators
 *  org.skriptlang.skript.lang.comparator.Relation
 *  org.skriptlang.skript.registration.SyntaxInfo
 *  org.skriptlang.skript.registration.SyntaxRegistry
 *  org.skriptlang.skript.util.Priority
 */
package com.btk5h.skriptmirror;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Version;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.LibraryLoader;
import com.btk5h.skriptmirror.Metrics;
import com.btk5h.skriptmirror.ParseOrderWorkarounds;
import com.btk5h.skriptmirror.skript.CondExpressionStatement;
import com.btk5h.skriptmirror.skript.EffExpressionStatement;
import com.btk5h.skriptmirror.skript.reflect.ExprJavaCall;
import com.btk5h.skriptmirror.skript.reflect.ExprProxy;
import com.btk5h.skriptmirror.skript.reflect.sections.SecSection;
import com.btk5h.skriptmirror.util.SkriptReflection;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.nio.file.Path;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.reflect.syntax.condition.elements.StructCustomCondition;
import org.skriptlang.reflect.syntax.effect.elements.StructCustomEffect;
import org.skriptlang.reflect.syntax.event.elements.ExprCustomEventValue;
import org.skriptlang.reflect.syntax.event.elements.StructCustomEvent;
import org.skriptlang.reflect.syntax.expression.elements.StructCustomExpression;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Priority;

public class SkriptMirror
extends JavaPlugin {
    private static SkriptMirror instance;
    private static SkriptAddon addonInstance;
    public static final Priority SHADOW_REALM;

    public SkriptMirror() {
        if (instance != null) {
            throw new IllegalStateException();
        }
        instance = this;
    }

    public void onEnable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Skript")) {
            this.getLogger().severe("Disabling skript-reflect because Skript is disabled");
            Bukkit.getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        if (!Skript.getVersion().isLargerThan(new Version("2.13.2"))) {
            this.getLogger().severe("");
            this.getLogger().severe("Your version of Skript (" + String.valueOf(Skript.getVersion()) + ") is not supported, at least Skript 2.14 is required to run this version of skript-reflect.");
            this.getLogger().severe("");
            Bukkit.getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("skript-mirror") != null) {
            this.getLogger().warning("You shouldn't have both skript-mirror and skript-reflect enabled, it will probably cause issues");
        }
        try {
            SkriptMirror.getAddonInstance().loadClasses("com.btk5h.skriptmirror.skript", new String[0]).loadClasses("org.skriptlang.reflect", new String[]{"syntax", "java.elements"});
            SyntaxRegistry registry = addonInstance.syntaxRegistry();
            ExprCustomEventValue.register(registry);
            ExprJavaCall.register(registry);
            CondExpressionStatement.register(registry);
            EffExpressionStatement.register(registry);
            Path dataFolder = SkriptMirror.getInstance().getDataFolder().toPath();
            LibraryLoader.loadLibraries(dataFolder);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        Comparators.registerComparator(ClassInfo.class, JavaType.class, (classInfo, javaType) -> {
            ClassInfo matchingClassInfo = Classes.getExactClassInfo(javaType.getJavaClass());
            if (matchingClassInfo == null) {
                return Relation.NOT_EQUAL;
            }
            return Comparators.compare((Object)classInfo, (Object)matchingClassInfo);
        });
        ParseOrderWorkarounds.reorderSyntax();
        SkriptReflection.disableAndOrWarnings();
        Metrics metrics = new Metrics((Plugin)this, 10157);
        metrics.addCustomChart(new Metrics.DrilldownPie("skript_version", () -> {
            HashMap map = new HashMap();
            Version version = Skript.getVersion();
            HashMap<String, Integer> entry = new HashMap<String, Integer>();
            entry.put(version.toString(), 1);
            map.put((CallSite)((Object)(version.getMajor() + "." + version.getMinor())), entry);
            return map;
        }));
        metrics.addCustomChart(new Metrics.SingleLineChart("java_calls_made", () -> {
            int i = ExprJavaCall.javaCallsMade;
            ExprJavaCall.javaCallsMade = 0;
            return i;
        }));
        metrics.addCustomChart(new Metrics.SimplePie("custom_conditions_used", () -> "" + StructCustomCondition.customConditionsUsed));
        metrics.addCustomChart(new Metrics.SimplePie("custom_effects_used", () -> "" + StructCustomEffect.customEffectsUsed));
        metrics.addCustomChart(new Metrics.SimplePie("custom_events_used", () -> "" + StructCustomEvent.customEventsUsed));
        metrics.addCustomChart(new Metrics.SimplePie("custom_expressions_used", () -> "" + StructCustomExpression.customExpressionsUsed));
        metrics.addCustomChart(new Metrics.SimplePie("proxies_used", () -> "" + ExprProxy.proxiesUsed));
        metrics.addCustomChart(new Metrics.SimplePie("sections_used", () -> "" + SecSection.sectionsUsed));
    }

    public static SkriptAddon getAddonInstance() {
        if (addonInstance == null) {
            addonInstance = Skript.registerAddon((JavaPlugin)SkriptMirror.getInstance()).setLanguageFileDirectory("lang");
        }
        return addonInstance;
    }

    public static SkriptMirror getInstance() {
        if (instance == null) {
            throw new IllegalStateException();
        }
        return instance;
    }

    static {
        SHADOW_REALM = Priority.after((Priority)SyntaxInfo.PATTERN_MATCHES_EVERYTHING);
    }
}

