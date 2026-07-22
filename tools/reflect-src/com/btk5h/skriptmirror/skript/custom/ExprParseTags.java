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
 *  org.jetbrains.annotations.Nullable
 */
package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;

public class ExprParseTags
extends SimpleExpression<String> {
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent(new Class[]{SyntaxParseEvent.class, ConditionCheckEvent.class, EffectTriggerEvent.class, EventTriggerEvent.class, ExpressionChangeEvent.class, ExpressionGetEvent.class})) {
            Skript.error((String)"The parse tags may only be used in custom syntax");
            return false;
        }
        return true;
    }

    @Nullable
    protected String[] get(Event e) {
        return ((CustomSyntaxEvent)e).getParseResult().tags.toArray(new String[0]);
    }

    public boolean isSingle() {
        return false;
    }

    public Class<? extends String> getReturnType() {
        return String.class;
    }

    public String toString(@Nullable Event e, boolean debug) {
        return "parse tags";
    }

    static {
        Skript.registerExpression(ExprParseTags.class, String.class, (ExpressionType)ExpressionType.SIMPLE, (String[])new String[]{"[the] parse[r] tags"});
    }
}

