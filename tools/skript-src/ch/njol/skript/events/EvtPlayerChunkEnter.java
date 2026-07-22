/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.Nullable;

public class EvtPlayerChunkEnter
extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        PlayerMoveEvent moveEvent = (PlayerMoveEvent)event;
        return !moveEvent.getFrom().getChunk().equals((Object)moveEvent.getTo().getChunk());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "player enter chunk";
    }

    static {
        Skript.registerEvent("Player Chunk Enter", EvtPlayerChunkEnter.class, PlayerMoveEvent.class, "[player] (enter[s] [a] chunk|chunk enter[ing])").description("Called when a player enters a chunk. Note that this event is based on 'player move' event, and may be called frequent internally.").examples("on player enters a chunk:", "\tsend \"You entered a chunk: %past event-chunk% -> %event-chunk%!\" to player").since("2.7");
    }
}

