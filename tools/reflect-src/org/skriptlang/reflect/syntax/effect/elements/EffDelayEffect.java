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
package org.skriptlang.reflect.syntax.effect.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;

public class EffDelayEffect
extends Effect {
    protected void execute(Event e) {
        ((EffectTriggerEvent)e).setSync(false);
    }

    public String toString(Event e, boolean debug) {
        return "delay effect";
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent(EffectTriggerEvent.class)) {
            Skript.error((String)"The effect 'delay effect' may only be used in a custom effect.", (ErrorQuality)ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        return true;
    }

    static {
        Skript.registerEffect(EffDelayEffect.class, (String[])new String[]{"delay [the] [current] effect"});
    }
}

