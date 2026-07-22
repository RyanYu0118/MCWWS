/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerGameModeChangeEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import java.util.Locale;
import org.bukkit.GameMode;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.jetbrains.annotations.Nullable;

public final class EvtGameMode
extends SkriptEvent {
    @Nullable
    private Literal<GameMode> mode;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
        this.mode = args[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        if (this.mode != null) {
            return this.mode.check(e, m -> ((PlayerGameModeChangeEvent)e).getNewGameMode().equals(m));
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "gamemode change" + (String)(this.mode != null ? " to " + this.mode.toString().toLowerCase(Locale.ENGLISH) : "");
    }

    static {
        Skript.registerEvent("Gamemode Change", EvtGameMode.class, PlayerGameModeChangeEvent.class, "game[ ]mode change [to %gamemode%]").description("Called when a player's <a href='#gamemode'>gamemode</a> changes.").examples("on gamemode change:", "on gamemode change to adventure:").since("1.0");
    }
}

