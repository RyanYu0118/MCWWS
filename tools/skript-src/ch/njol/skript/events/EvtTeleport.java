/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityTeleportEvent
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventConverter;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.Nullable;

public class EvtTeleport
extends SkriptEvent {
    @Nullable
    private Literal<EntityType> entitiesLiteral;
    private EntityType @Nullable [] entities;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (args[0] != null) {
            this.entitiesLiteral = args[0];
            this.entities = this.entitiesLiteral.getAll();
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (event instanceof EntityTeleportEvent) {
            Entity entity = ((EntityTeleportEvent)event).getEntity();
            return this.checkEntity(entity);
        }
        if (event instanceof PlayerTeleportEvent) {
            Player entity = ((PlayerTeleportEvent)event).getPlayer();
            return this.checkEntity((Entity)entity);
        }
        return false;
    }

    private boolean checkEntity(Entity entity) {
        if (this.entities != null) {
            for (EntityType entType : this.entities) {
                if (!entType.isInstance(entity)) continue;
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.entitiesLiteral != null) {
            return "on " + this.entitiesLiteral.toString(event, debug) + " teleport";
        }
        return "on teleport";
    }

    static {
        Skript.registerEvent("Teleport", EvtTeleport.class, CollectionUtils.array(EntityTeleportEvent.class, PlayerTeleportEvent.class), "[%entitytypes%] teleport[ing]").description("This event can be used to listen to teleports from non-players or player entities respectively.", "When teleporting entities, the event may also be called due to a result of natural causes, such as an enderman or shulker teleporting, or wolves teleporting to players.", "When teleporting players, the event can be called by teleporting through a nether/end portal, or by other means (e.g. plugins).").examples("on teleport:", "on player teleport:", "on creeper teleport:").since("1.0, 2.9.0 (entity teleport)");
        EventValues.registerEventValue(PlayerTeleportEvent.class, Location.class, new EventConverter<PlayerTeleportEvent, Location>(){

            @Override
            public void set(PlayerTeleportEvent event, Location value) {
                event.setFrom(value.clone());
            }

            @Override
            public Location convert(PlayerTeleportEvent event) {
                return event.getFrom();
            }
        }, EventValues.TIME_PAST);
        EventValues.registerEventValue(PlayerTeleportEvent.class, Location.class, new EventConverter<PlayerTeleportEvent, Location>(){

            @Override
            public void set(PlayerTeleportEvent event, Location value) {
                event.setTo(value.clone());
            }

            @Override
            public Location convert(PlayerTeleportEvent event) {
                return event.getTo();
            }
        });
        EventValues.registerEventValue(EntityTeleportEvent.class, Location.class, new EventConverter<EntityTeleportEvent, Location>(){

            @Override
            public void set(EntityTeleportEvent event, Location value) {
                event.setFrom(value.clone());
            }

            @Override
            public Location convert(EntityTeleportEvent event) {
                return event.getFrom();
            }
        }, EventValues.TIME_PAST);
        EventValues.registerEventValue(EntityTeleportEvent.class, Location.class, new EventConverter<EntityTeleportEvent, Location>(){

            @Override
            public void set(EntityTeleportEvent event, Location value) {
                event.setTo(value.clone());
            }

            @Override
            public Location convert(EntityTeleportEvent event) {
                return event.getTo();
            }
        });
    }
}

