/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.common.function;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.Parameter;

public record ScriptParameter<T>(String name, Class<T> type, Set<Parameter.Modifier> modifiers, @Nullable Expression<?> defaultValue) implements Parameter<T>
{
    public ScriptParameter(String name, Class<T> type, Parameter.Modifier ... modifiers) {
        this(name, type, Set.of(modifiers), null);
    }

    public ScriptParameter(String name, Class<T> type, Expression<?> defaultValue, Parameter.Modifier ... modifiers) {
        this(name, type, Set.of(modifiers), defaultValue);
    }

    public static Parameter<?> parse(@NotNull String name, @NotNull Class<?> type, @Nullable String def) {
        Preconditions.checkNotNull((Object)name, (Object)"name cannot be null");
        Preconditions.checkNotNull(type, (Object)"type cannot be null");
        if (!Variable.isValidVariableName(name, true, false)) {
            Skript.error("Invalid parameter name: %s", name);
            return null;
        }
        Expression defaultValue = null;
        if (def != null) {
            Class<?> target = Utils.getComponentType(type);
            try (RetainingLogHandler log = SkriptLogger.startRetainingLog();){
                defaultValue = new SkriptParser(def, 3, ParseContext.DEFAULT).parseExpression(target);
                if (defaultValue == null || LiteralUtils.hasUnparsedLiteral(defaultValue)) {
                    log.printErrors("Can't understand this expression: " + def);
                    log.stop();
                    Parameter<?> parameter = null;
                    return parameter;
                }
                log.printLog();
                log.stop();
            }
        }
        HashSet<Parameter.Modifier> modifiers = new HashSet<Parameter.Modifier>();
        if (defaultValue != null) {
            modifiers.add(Parameter.Modifier.OPTIONAL);
        }
        if (type.isArray()) {
            modifiers.add(Parameter.Modifier.KEYED);
        }
        return new ScriptParameter(name, type, defaultValue, modifiers.toArray(new Parameter.Modifier[0]));
    }

    @Override
    public Object[] evaluate(@Nullable Expression<? extends T> argument, Event event) {
        if (argument == null) {
            if (!this.hasModifier(Parameter.Modifier.OPTIONAL)) {
                throw new IllegalStateException("This parameter is required, but no argument was provided");
            }
            if (this.defaultValue == null) {
                throw new IllegalStateException("This parameter does not have a default value");
            }
            return Parameter.super.evaluate(this.defaultValue, event);
        }
        return Parameter.super.evaluate(argument, event);
    }

    @Override
    @NotNull
    public String toString() {
        return this.toFormattedString();
    }
}

