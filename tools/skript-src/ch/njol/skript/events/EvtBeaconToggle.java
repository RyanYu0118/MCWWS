/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.block.BeaconActivatedEvent
 *  io.papermc.paper.event.block.BeaconDeactivatedEvent
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import io.papermc.paper.event.block.BeaconActivatedEvent;
import io.papermc.paper.event.block.BeaconDeactivatedEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EvtBeaconToggle
extends SkriptEvent {
    private boolean isActivate;
    private boolean isToggle;

    @Override
    public boolean init(Literal<?>[] exprs, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.isToggle = matchedPattern == 0;
        this.isActivate = matchedPattern == 1;
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (!this.isToggle) {
            if (event instanceof BeaconActivatedEvent) {
                return this.isActivate;
            }
            if (event instanceof BeaconDeactivatedEvent) {
                return !this.isActivate;
            }
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "beacon " + (this.isToggle ? "toggle" : (this.isActivate ? "activate" : "deactivate"));
    }

    static {
        if (Skript.classExists("io.papermc.paper.event.block.BeaconActivatedEvent")) {
            Skript.registerEvent("Beacon Toggle", EvtBeaconToggle.class, new Class[]{BeaconActivatedEvent.class, BeaconDeactivatedEvent.class}, "beacon toggle", "beacon activat(e|ion)", "beacon deactivat(e|ion)").description("Called when a beacon is activated or deactivated.").examples("on beacon toggle:", "on beacon activate:", "on beacon deactivate:").since("2.10");
        }
    }
}

