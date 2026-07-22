/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Effect
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.log.ErrorQuality
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 */
package org.skriptlang.reflect.syntax.condition.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;

public class EffNegateCondition
extends Effect {
    protected void execute(Event e) {
        ((ConditionCheckEvent)e).markNegated();
    }

    public String toString(Event e, boolean debug) {
        return "negate condition";
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent(ConditionCheckEvent.class)) {
            Skript.error((String)"The effect 'negate condition' may only be used in a custom condition.", (ErrorQuality)ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        return true;
    }

    static {
        Skript.registerEffect(EffNegateCondition.class, (String[])new String[]{"negate [the] [current] condition"});
    }
}

