/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.parser.ParserInstance
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package org.skriptlang.reflect.syntax.expression.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import java.util.Arrays;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;

public class ExprChangeValue
extends SimpleExpression<Object> {
    private boolean isPlural;
    private Class<?> returnType;

    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        ParserInstance parser = this.getParser();
        if (!parser.isCurrentEvent(ExpressionChangeEvent.class)) {
            Skript.error((String)"The change value may only be used in a change handler");
            return false;
        }
        this.isPlural = parseResult.hasTag("plural");
        this.returnType = Object.class;
        return true;
    }

    protected Object[] get(Event event) {
        Object[] delta = ((ExpressionChangeEvent)event).getDelta();
        if (delta == null) {
            return (Object[])Array.newInstance(this.getReturnType(), 0);
        }
        return Arrays.copyOf(delta, delta.length, (Class<? extends T[]>)this.getReturnType().arrayType());
    }

    public boolean isSingle() {
        return !this.isPlural;
    }

    public Class<?> getReturnType() {
        return this.returnType;
    }

    public String toString(Event event, boolean debug) {
        return "the change value" + (this.isPlural ? "s" : "");
    }

    static {
        Skript.registerExpression(ExprChangeValue.class, Object.class, (ExpressionType)ExpressionType.SIMPLE, (String[])new String[]{"[the] change value[plural:s]"});
    }
}

