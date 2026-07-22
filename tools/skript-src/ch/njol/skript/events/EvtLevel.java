/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerLevelChangeEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.jetbrains.annotations.Nullable;

public class EvtLevel
extends SkriptEvent {
    private Kleenean leveling;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.leveling = Kleenean.get(parseResult.mark);
        return true;
    }

    @Override
    public boolean check(Event e) {
        PlayerLevelChangeEvent event = (PlayerLevelChangeEvent)e;
        if (this.leveling.isTrue()) {
            return event.getNewLevel() > event.getOldLevel();
        }
        if (this.leveling.isFalse()) {
            return event.getNewLevel() < event.getOldLevel();
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "level " + (this.leveling.isTrue() ? "up" : (this.leveling.isFalse() ? "down" : "change"));
    }

    static {
        Skript.registerEvent("Level Change", EvtLevel.class, PlayerLevelChangeEvent.class, "[player] level (change|1\u00a6up|-1\u00a6down)").description("Called when a player's <a href='#ExprLevel'>level</a> changes, e.g. by gathering experience or by enchanting something.").examples("on level change:").since("1.0, 2.4 (level up/down)");
    }
}

