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
 *  ch.njol.skript.util.Utils
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
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.skript.custom.CustomSyntaxEvent;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import java.util.List;
import java.util.regex.MatchResult;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;

public class ExprParseRegex
extends SimpleExpression<String> {
    private int index;

    protected String[] get(Event e) {
        List regexes = ((CustomSyntaxEvent)e).getParseResult().regexes;
        if (this.index < regexes.size()) {
            MatchResult match = (MatchResult)regexes.get(this.index);
            int groupCount = match.groupCount();
            String[] groups = new String[groupCount];
            for (int i = 1; i <= groupCount; ++i) {
                groups[i - 1] = match.group(i);
            }
            return groups;
        }
        return new String[0];
    }

    public boolean isSingle() {
        return false;
    }

    public Class<? extends String> getReturnType() {
        return String.class;
    }

    public String toString(Event e, boolean debug) {
        return "parser mark";
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent(new Class[]{SyntaxParseEvent.class, ConditionCheckEvent.class, EffectTriggerEvent.class, EventTriggerEvent.class, ExpressionChangeEvent.class, ExpressionGetEvent.class})) {
            Skript.error((String)"The parsed regular expression may only be used in custom syntax.", (ErrorQuality)ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        this.index = Utils.parseInt((String)((MatchResult)parseResult.regexes.get(0)).group(0));
        if (this.index <= 0) {
            Skript.error((String)"The expression index must be a natural number.", (ErrorQuality)ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        --this.index;
        return true;
    }

    static {
        Skript.registerExpression(ExprParseRegex.class, String.class, (ExpressionType)ExpressionType.SIMPLE, (String[])new String[]{"[the] [parse[r]] (regex|regular expression)(-| )<\\d+>"});
    }
}

