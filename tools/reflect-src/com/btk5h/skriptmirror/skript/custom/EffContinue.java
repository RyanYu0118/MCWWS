/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Effect
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.log.ErrorQuality
 *  ch.njol.util.Kleenean
 *  ch.njol.util.coll.CollectionUtils
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror.skript.custom;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.btk5h.skriptmirror.skript.custom.Continuable;
import org.bukkit.event.Event;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;

public class EffContinue
extends Effect {
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent(EffectTriggerEvent.class) && !CollectionUtils.containsAnySuperclass((Class[])new Class[]{Continuable.class}, (Class[])this.getParser().getCurrentEvents())) {
            Skript.error((String)"Continue may only be used in loops, custom effects, custom syntax parse sections and custom conditions", (ErrorQuality)ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        return true;
    }

    protected void execute(Event e) {
        throw new UnsupportedOperationException();
    }

    protected TriggerItem walk(Event e) {
        if (e instanceof EffectTriggerEvent) {
            EffectTriggerEvent effectTriggerEvent = (EffectTriggerEvent)e;
            if (effectTriggerEvent.isSync()) {
                Skript.warning((String)"Synchronous events should not be continued. Call 'delay effect' to delay the effect's execution.");
            } else {
                effectTriggerEvent.setContinued();
                TriggerItem.walk((TriggerItem)effectTriggerEvent.getNext(), (Event)effectTriggerEvent.getDirectEvent());
            }
        } else if (e instanceof Continuable) {
            ((Continuable)e).markContinue();
        }
        return null;
    }

    public String toString(Event e, boolean debug) {
        return "continue";
    }

    static {
        Skript.registerEffect(EffContinue.class, (String[])new String[]{"continue"});
    }
}

