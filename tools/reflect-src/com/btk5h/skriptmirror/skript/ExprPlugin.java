/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.expressions.base.SimplePropertyExpression
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.Literal
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.util.Kleenean
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.jetbrains.annotations.NotNull
 */
package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.ObjectWrapper;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class ExprPlugin
extends SimplePropertyExpression<Object, ObjectWrapper> {
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        JavaType javaType;
        Class<?> clazz;
        Literal literal;
        Object literalValue;
        if (!super.init(exprs, matchedPattern, isDelayed, parseResult)) {
            return false;
        }
        Expression expression = this.getExpr();
        if (expression instanceof Literal && (literalValue = (literal = (Literal)expression).getSingle()) instanceof JavaType && (!JavaPlugin.class.isAssignableFrom(clazz = (javaType = (JavaType)literalValue).getJavaClass()) || JavaPlugin.class.equals(clazz))) {
            Skript.error((String)("The class " + clazz.getSimpleName() + " is not a plugin class"));
            return false;
        }
        return true;
    }

    public ObjectWrapper convert(Object plugin) {
        if (plugin instanceof String) {
            String pluginName = (String)plugin;
            for (Plugin pluginInstance : Bukkit.getPluginManager().getPlugins()) {
                if (!pluginInstance.getName().equalsIgnoreCase(pluginName)) continue;
                return ObjectWrapper.create(pluginInstance);
            }
            return null;
        }
        Class<?> clazz = ((JavaType)plugin).getJavaClass();
        if (!JavaPlugin.class.isAssignableFrom(clazz) || JavaPlugin.class.equals(clazz)) {
            return null;
        }
        return ObjectWrapper.create(JavaPlugin.getPlugin(clazz.asSubclass(JavaPlugin.class)));
    }

    @NotNull
    public Class<? extends ObjectWrapper> getReturnType() {
        return ObjectWrapper.class;
    }

    @NotNull
    protected String getPropertyName() {
        return "plugin instance";
    }

    static {
        Skript.registerExpression(ExprPlugin.class, ObjectWrapper.class, (ExpressionType)ExpressionType.PROPERTY, (String[])new String[]{"[(an|the)] instance of [the] plugin %javatype/string%"});
    }
}

