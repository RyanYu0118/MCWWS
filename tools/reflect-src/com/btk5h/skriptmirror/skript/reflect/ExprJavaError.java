/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror.skript.reflect;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.reflect.ExprJavaCall;
import org.bukkit.event.Event;

public class ExprJavaError
extends SimpleExpression<Throwable> {
    protected Throwable[] get(Event e) {
        return new Throwable[]{ExprJavaCall.lastError};
    }

    public boolean isSingle() {
        return true;
    }

    public Class<? extends Throwable> getReturnType() {
        return Throwable.class;
    }

    public String toString(Event e, boolean debug) {
        return "last java error";
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    static {
        Skript.registerExpression(ExprJavaError.class, Throwable.class, (ExpressionType)ExpressionType.SIMPLE, (String[])new String[]{"[the] [last] [java] (throwable|exception|error)"});
    }
}

