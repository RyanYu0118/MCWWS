/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.skript.util.Utils
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.regex.MatchResult;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;

public class ExprExpression
extends SimpleExpression<Object> {
    private int index;
    private boolean isPlural;
    private Class<?> returnType;

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent(new Class[]{SyntaxParseEvent.class, ConditionCheckEvent.class, EffectTriggerEvent.class, EventTriggerEvent.class, ExpressionChangeEvent.class, ExpressionGetEvent.class})) {
            Skript.error((String)"The expression 'expression' may only be used in a custom syntax structure");
            return false;
        }
        this.index = Utils.parseInt((String)((MatchResult)parseResult.regexes.getFirst()).group(0));
        if (this.index <= 0) {
            Skript.error((String)"The expression index must be a natural number");
            return false;
        }
        --this.index;
        this.isPlural = parseResult.hasTag("plural");
        this.returnType = Object.class;
        return true;
    }

    protected Object @Nullable [] get(Event event) {
        Expression<?> expr = this.getExpression(event);
        if (expr == null) {
            return (Object[])Array.newInstance(this.getReturnType(), 0);
        }
        Object[] values = expr.getAll(event);
        return Arrays.copyOf(values, values.length, (Class<? extends T[]>)this.getReturnType().arrayType());
    }

    @Nullable
    Expression<?> getExpression(Event event) {
        Expression<?>[] expressions = ((CustomSyntaxEvent)event).getExpressions();
        if (this.index < expressions.length) {
            return expressions[this.index];
        }
        return null;
    }

    public boolean isSingle() {
        return !this.isPlural;
    }

    public Class<?> getReturnType() {
        return this.returnType;
    }

    public String toString(Event event, boolean debug) {
        return "expression " + (this.index + 1);
    }

    static {
        Skript.registerExpression(ExprExpression.class, Object.class, (ExpressionType)ExpressionType.SIMPLE, (String[])new String[]{"[the] expr[ession][plural:s](-| )<\\d+>"});
    }
}

