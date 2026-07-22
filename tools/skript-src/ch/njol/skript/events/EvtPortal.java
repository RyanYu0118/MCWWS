/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityPortalEvent
 *  org.bukkit.event.player.PlayerPortalEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.jetbrains.annotations.Nullable;

public class EvtPortal
extends SkriptEvent {
    private boolean isPlayer;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.isPlayer = matchedPattern == 0;
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (this.isPlayer) {
            return event instanceof PlayerPortalEvent;
        }
        return event instanceof EntityPortalEvent;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.isPlayer ? "player" : "entity") + " portal";
    }

    static {
        Skript.registerEvent("Portal", EvtPortal.class, CollectionUtils.array(PlayerPortalEvent.class, EntityPortalEvent.class), "[player] portal", "entity portal").description("Called when a player or an entity uses a nether or end portal. Note that 'on entity portal' event does not apply to players.", "<a href='#EffCancelEvent'>Cancel the event</a> to prevent the entity from teleporting.").keywords("player", "entity").examples("on portal:", "\tbroadcast \"%player% has entered a portal!\"", "", "on player portal:", "\tplayer's world is world(\"wilderness\")", "\tset world of event-location to player's world", "\tadd 9000 to x-pos of event-location", "", "on entity portal:", "\tbroadcast \"A %type of event-entity% has entered a portal!").since("1.0, 2.5.3 (entities), 2.13 (location changers)");
    }
}

