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
package com.btk5h.skriptmirror.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.FunctionWrapper;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.Arrays;
import org.bukkit.event.Event;

public class ExprFunction
extends SimpleExpression<FunctionWrapper> {
    private Expression<String> refs;
    private Expression<Object> args;

    protected FunctionWrapper[] get(Event e) {
        Object[] functionArgs = this.args == null ? new Object[]{} : this.args.getArray(e);
        return (FunctionWrapper[])Arrays.stream((String[])this.refs.getArray(e)).map(ref -> new FunctionWrapper((String)ref, functionArgs)).toArray(FunctionWrapper[]::new);
    }

    public boolean isSingle() {
        return this.refs.isSingle();
    }

    public Class<? extends FunctionWrapper> getReturnType() {
        return FunctionWrapper.class;
    }

    public String toString(Event e, boolean debug) {
        return "function reference of " + this.refs.toString(e, debug);
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.refs = SkriptUtil.defendExpression(exprs[0]);
        if (exprs[1] != null) {
            this.args = SkriptUtil.defendExpression(exprs[1]);
        }
        return SkriptUtil.canInitSafely(this.args);
    }

    static {
        Skript.registerExpression(ExprFunction.class, FunctionWrapper.class, (ExpressionType)ExpressionType.PROPERTY, (String[])new String[]{"[the] function(s| [reference[s]]) %strings% [called with [[the] [arg[ument][s]]] %-objects%]"});
    }
}

