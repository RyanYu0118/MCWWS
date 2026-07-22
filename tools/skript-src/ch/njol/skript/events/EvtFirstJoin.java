/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

public class EvtFirstJoin
extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        return true;
    }

    @Override
    public boolean check(Event e) {
        return !((PlayerJoinEvent)e).getPlayer().hasPlayedBefore();
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "first join";
    }

    static {
        Skript.registerEvent("First Join", EvtFirstJoin.class, PlayerJoinEvent.class, "first (join|login)").description("Called when a player joins the server for the first time.").examples("on first join:", "\tbroadcast \"Welcome %player% to the server!\"").since("1.3.7");
    }
}

