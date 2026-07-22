/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.entity.EntityMoveEvent
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.coll.CollectionUtils;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.Nullable;

public class EvtMove
extends SkriptEvent {
    private static final boolean HAS_ENTITY_MOVE = Skript.classExists("io.papermc.paper.event.entity.EntityMoveEvent");
    private EntityData<?> entityData;
    private boolean isPlayer;
    private boolean canBePlayer;
    private Move moveType;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.entityData = (EntityData)args[0].getSingle();
        this.isPlayer = Player.class.isAssignableFrom(this.entityData.getType());
        if (!HAS_ENTITY_MOVE && !this.isPlayer) {
            Skript.error("Entity move event requires Paper");
            return false;
        }
        this.canBePlayer = this.entityData.getType().isAssignableFrom(Player.class);
        this.moveType = matchedPattern > 0 ? Move.MOVE_OR_ROTATE : (parseResult.hasTag("rotate") ? Move.ROTATE : Move.MOVE);
        return true;
    }

    @Override
    public boolean check(Event event) {
        Location to;
        Location from;
        if (this.canBePlayer && event instanceof PlayerMoveEvent) {
            PlayerMoveEvent playerMoveEvent = (PlayerMoveEvent)event;
            from = playerMoveEvent.getFrom();
            to = playerMoveEvent.getTo();
        } else if (HAS_ENTITY_MOVE && event instanceof EntityMoveEvent) {
            EntityMoveEvent entityMoveEvent = (EntityMoveEvent)event;
            if (!this.entityData.isInstance((Entity)entityMoveEvent.getEntity())) {
                return false;
            }
            from = entityMoveEvent.getFrom();
            to = entityMoveEvent.getTo();
        } else {
            return false;
        }
        return switch (this.moveType.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> EvtMove.hasChangedPosition(from, to);
            case 2 -> EvtMove.hasChangedOrientation(from, to);
            case 1 -> true;
        };
    }

    @Override
    public Class<? extends Event>[] getEventClasses() {
        if (this.isPlayer) {
            return new Class[]{PlayerMoveEvent.class};
        }
        if (HAS_ENTITY_MOVE) {
            if (this.canBePlayer) {
                return new Class[]{EntityMoveEvent.class, PlayerMoveEvent.class};
            }
            return new Class[]{EntityMoveEvent.class};
        }
        throw new IllegalStateException("This event has not yet initialized!");
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return String.valueOf(this.entityData) + " " + String.valueOf((Object)this.moveType);
    }

    private static boolean hasChangedPosition(Location from, Location to) {
        return from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ() || from.getWorld() != to.getWorld();
    }

    private static boolean hasChangedOrientation(Location from, Location to) {
        return from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch();
    }

    static {
        Class[] events = HAS_ENTITY_MOVE ? CollectionUtils.array(PlayerMoveEvent.class, EntityMoveEvent.class) : CollectionUtils.array(PlayerMoveEvent.class);
        Skript.registerEvent("Move / Rotate", EvtMove.class, events, "%entitydata% (move|walk|step|rotate:(turn[ing] around|rotate))", "%entitydata% (move|walk|step) or (turn[ing] around|rotate)", "%entitydata% (turn[ing] around|rotate) or (move|walk|step)").description("Called when a player or entity moves or rotates their head.", "NOTE: Move event will only be called when the entity/player moves position, keyword 'turn around' is for orientation (ie: looking around), and the combined syntax listens for both.", "NOTE: These events can be performance heavy as they are called quite often.").examples("on player move:", "\tif player does not have permission \"player.can.move\":", "\t\tcancel event", "on skeleton move:", "\tif event-entity is not in world \"world\":", "\t\tkill event-entity", "on player turning around:", "\tsend action bar \"You are currently turning your head around!\" to player").since("2.6, 2.8.0 (turn around)");
    }

    private static enum Move {
        MOVE("move"),
        MOVE_OR_ROTATE("move or rotate"),
        ROTATE("rotate");

        private final String name;

        private Move(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }
}

