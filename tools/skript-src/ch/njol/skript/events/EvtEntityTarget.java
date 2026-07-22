/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityTargetEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTargetEvent;
import org.jetbrains.annotations.Nullable;

public class EvtEntityTarget
extends SkriptEvent {
    private boolean target;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        this.target = matchedPattern == 0;
        return true;
    }

    @Override
    public boolean check(Event e) {
        return ((EntityTargetEvent)e).getTarget() == null ^ this.target;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "entity " + (this.target ? "" : "un") + "target";
    }

    static {
        Skript.registerEvent("Target", EvtEntityTarget.class, EntityTargetEvent.class, "[entity] target", "[entity] un[-]target").description("Called when a mob starts/stops following/attacking another entity, usually a player.").examples("on entity target:", "\ttarget is a player").since("1.0");
    }
}

