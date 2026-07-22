/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerExpChangeEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Experience;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.jetbrains.annotations.Nullable;

public class EvtExperienceChange
extends SkriptEvent {
    private static final int ANY = 0;
    private static final int UP = 1;
    private static final int DOWN = 2;
    private int mode = 0;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (parseResult.hasTag("increase")) {
            this.mode = 1;
        } else if (parseResult.hasTag("decrease")) {
            this.mode = 2;
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (this.mode == 0) {
            return true;
        }
        PlayerExpChangeEvent expChangeEvent = (PlayerExpChangeEvent)event;
        return this.mode == 1 ? expChangeEvent.getAmount() > 0 : expChangeEvent.getAmount() < 0;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "player level progress " + (this.mode == 0 ? "change" : (this.mode == 1 ? "increase" : "decrease"));
    }

    static {
        Skript.registerEvent("Experience Change", EvtExperienceChange.class, PlayerExpChangeEvent.class, "[player] (level progress|[e]xp|experience) (change|update|:increase|:decrease)").description("Called when a player's experience changes.").examples("on level progress change:", "\tset {_xp} to event-experience", "\tbroadcast \"%{_xp}%\"").since("2.7");
        EventValues.registerEventValue(PlayerExpChangeEvent.class, Experience.class, event -> new Experience(event.getAmount()));
    }
}

