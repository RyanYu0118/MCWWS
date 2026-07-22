/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.SkriptConfig
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.config.Node
 *  ch.njol.skript.config.Option
 *  ch.njol.skript.config.SectionNode
 *  ch.njol.skript.expressions.base.EventValueExpression
 *  ch.njol.skript.lang.DefaultExpression
 *  ch.njol.skript.registrations.Classes
 *  ch.njol.skript.structures.StructOptions$OptionsData
 *  ch.njol.skript.variables.Variables
 *  org.bukkit.event.Event
 *  org.skriptlang.skript.lang.script.Script
 */
package com.btk5h.skriptmirror.util;

import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.Option;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.structures.StructOptions;
import ch.njol.skript.variables.Variables;
import com.btk5h.skriptmirror.SkriptMirror;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.event.elements.ExprReplacedEventValue;
import org.skriptlang.skript.lang.script.Script;

public class SkriptReflection {
    private static Field LOCAL_VARIABLES;
    private static Field NODES;
    private static Method VARIABLES_MAP_COPY;
    private static Field DEFAULT_EXPRESSION;
    private static Field PARSED_VALUE;
    private static Field OPTIONS;

    private static void warning(String message) {
        SkriptMirror.getInstance().getLogger().warning(message);
    }

    public static void putLocals(Object originalVariablesMap, Event to) {
        if (originalVariablesMap == null) {
            SkriptReflection.removeLocals(to);
            return;
        }
        try {
            Map localVariables = (Map)LOCAL_VARIABLES.get(null);
            localVariables.put(to, originalVariablesMap);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object removeLocals(Event event) {
        try {
            Map localVariables = (Map)LOCAL_VARIABLES.get(null);
            return localVariables.remove(event);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getLocals(Event event) {
        try {
            Map localVariables = (Map)LOCAL_VARIABLES.get(null);
            return localVariables.get(event);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object copyLocals(Object locals) {
        if (locals == null) {
            return null;
        }
        try {
            return VARIABLES_MAP_COPY.invoke(locals, new Object[0]);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static ArrayList<Node> getNodes(SectionNode sectionNode) {
        if (NODES == null) {
            return new ArrayList<Node>();
        }
        try {
            return (ArrayList)NODES.get(sectionNode);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void replaceEventValues(List<ClassInfo<?>> classInfoList) {
        if (DEFAULT_EXPRESSION == null) {
            return;
        }
        try {
            ArrayList replaceExtraList = new ArrayList();
            for (ClassInfo<?> classInfo : classInfoList) {
                DefaultExpression defaultExpression = classInfo.getDefaultExpression();
                if (!(defaultExpression instanceof EventValueExpression) || defaultExpression instanceof ExprReplacedEventValue) continue;
                DEFAULT_EXPRESSION.set(classInfo, (Object)new ExprReplacedEventValue((EventValueExpression)defaultExpression));
                replaceExtraList.add(classInfo);
            }
            replaceExtraList.forEach(SkriptReflection::replaceExtra);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void replaceExtra(ClassInfo<?> classInfo) {
        List<ClassInfo<?>> classInfoList = Classes.getClassInfos().stream().filter(loopedClassInfo -> !(loopedClassInfo.getDefaultExpression() instanceof ExprReplacedEventValue)).filter(loopedClassInfo -> classInfo.getC().isAssignableFrom(loopedClassInfo.getC()) || loopedClassInfo.getC().isAssignableFrom(classInfo.getC())).collect(Collectors.toList());
        SkriptReflection.replaceEventValues(classInfoList);
    }

    public static void disableAndOrWarnings() {
        if (PARSED_VALUE == null) {
            return;
        }
        Option option = SkriptConfig.disableMissingAndOrWarnings;
        if (!((Boolean)option.value()).booleanValue()) {
            try {
                PARSED_VALUE.set(option, true);
            }
            catch (IllegalAccessException e) {
                throw new RuntimeException();
            }
        }
    }

    public static Map<String, String> getOptions(Script script) {
        if (script == null) {
            throw new NullPointerException();
        }
        if (OPTIONS == null) {
            throw new IllegalStateException("OPTIONS field not initialized, computed options cannot be used");
        }
        StructOptions.OptionsData optionsData = (StructOptions.OptionsData)script.getData(StructOptions.OptionsData.class, StructOptions.OptionsData::new);
        try {
            return (Map)OPTIONS.get(optionsData);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    static {
        Field _FIELD;
        try {
            _FIELD = Variables.class.getDeclaredField("localVariables");
            _FIELD.setAccessible(true);
            LOCAL_VARIABLES = _FIELD;
        }
        catch (NoSuchFieldException e) {
            SkriptReflection.warning("Skript's local variables field could not be resolved.");
        }
        try {
            _FIELD = SectionNode.class.getDeclaredField("nodes");
            _FIELD.setAccessible(true);
            NODES = _FIELD;
        }
        catch (NoSuchFieldException e) {
            SkriptReflection.warning("Skript's nodes field could not be resolved, therefore custom syntax and sections won't work.");
        }
        try {
            Class<?> variablesMap = Class.forName("ch.njol.skript.variables.VariablesMap");
            try {
                Method _METHOD = variablesMap.getDeclaredMethod("copy", new Class[0]);
                _METHOD.setAccessible(true);
                VARIABLES_MAP_COPY = _METHOD;
            }
            catch (NoSuchMethodException e) {
                SkriptReflection.warning("Skript's variables map 'copy' method could not be resolved.");
            }
        }
        catch (ClassNotFoundException e) {
            SkriptReflection.warning("Skript's variables map class could not be resolved.");
        }
        try {
            _FIELD = ClassInfo.class.getDeclaredField("defaultExpression");
            _FIELD.setAccessible(true);
            DEFAULT_EXPRESSION = _FIELD;
        }
        catch (NoSuchFieldException e) {
            SkriptReflection.warning("Skript's default expression field could not be resolved, therefore event-values won't work in custom events");
        }
        try {
            _FIELD = Option.class.getDeclaredField("parsedValue");
            _FIELD.setAccessible(true);
            PARSED_VALUE = _FIELD;
        }
        catch (NoSuchFieldException e) {
            SkriptReflection.warning("Skript's parsed value field could not be resolved, therefore and/or warnings won't be suppressed");
        }
        try {
            _FIELD = StructOptions.OptionsData.class.getDeclaredField("options");
            _FIELD.setAccessible(true);
            OPTIONS = _FIELD;
        }
        catch (NoSuchFieldException e) {
            SkriptReflection.warning("Skript's options field could not be resolved, computed options won't work");
        }
    }
}

