/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.util.SimpleExpression
 *  ch.njol.skript.log.ErrorQuality
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;

public class ExprMatchedPattern
extends SimpleExpression<Number> {
    protected Number[] get(Event e) {
        return new Number[]{((CustomSyntaxEvent)e).getMatchedPattern()};
    }

    public boolean isSingle() {
        return true;
    }

    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    public String toString(Event e, boolean debug) {
        return "matched pattern";
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent(new Class[]{SyntaxParseEvent.class, ConditionCheckEvent.class, EffectTriggerEvent.class, EventTriggerEvent.class, ExpressionChangeEvent.class, ExpressionGetEvent.class})) {
            Skript.error((String)"The matched pattern may only be used in custom syntax.", (ErrorQuality)ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        return true;
    }

    static {
        Skript.registerExpression(ExprMatchedPattern.class, Number.class, (ExpressionType)ExpressionType.SIMPLE, (String[])new String[]{"[the] [matched] pattern"});
    }
}

