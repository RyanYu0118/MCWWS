/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.entity.TeleportFlag
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.SkriptTeleportFlag;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.paperlib.PaperLib;
import ch.njol.skript.paperlib.environments.PaperEnvironment;
import ch.njol.skript.sections.EffSecSpawn;
import ch.njol.skript.timings.SkriptTimings;
import ch.njol.skript.util.Direction;
import ch.njol.skript.variables.Variables;
import ch.njol.skript.variables.VariablesMap;
import ch.njol.util.Kleenean;
import io.papermc.paper.entity.TeleportFlag;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Name(value="Teleport")
@Description(value={"Teleport an entity to a specific location. ", "This effect is delayed by default on Paper, meaning certain syntax such as the return effect for functions cannot be used after this effect.", "The keyword 'force' indicates this effect will not be delayed, ", "which may cause lag spikes or server crashes when using this effect to teleport entities to unloaded chunks.", "Teleport flags are settings to retain during a teleport. Such as direction, passengers, x coordinate, etc."})
@Example.Examples(value={@Example(value="teleport the player to {home::%uuid of player%}"), @Example(value="teleport the attacker to the victim"), @Example(value="on dismount:\n\tcancel event\n\tteleport the player to {server::spawn} retaining vehicle and passengers\n")})
@Since(value={"1.0, 2.10 (flags)"})
public class EffTeleport
extends Effect {
    private static final boolean TELEPORT_FLAGS_SUPPORTED = Skript.classExists("io.papermc.paper.entity.TeleportFlag");
    private static final boolean CAN_RUN_ASYNC = PaperLib.getEnvironment() instanceof PaperEnvironment;
    @Nullable
    private Expression<SkriptTeleportFlag> teleportFlags;
    private Expression<Entity> entities;
    private Expression<Location> location;
    private boolean async;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        this.location = Direction.combine(exprs[1], exprs[2]);
        boolean bl = this.async = CAN_RUN_ASYNC && !parseResult.hasTag("force");
        if (TELEPORT_FLAGS_SUPPORTED) {
            this.teleportFlags = exprs[3];
        }
        if (this.getParser().isCurrentEvent((Class<? extends Event>)EffSecSpawn.SpawnEvent.class)) {
            Skript.error("You cannot teleport an entity that hasn't spawned yet. Ensure you're using the location expression from the spawn section pattern.");
            return false;
        }
        if (this.async) {
            this.getParser().setHasDelayBefore(Kleenean.UNKNOWN);
        }
        return true;
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        this.debug(event, true);
        TriggerItem next = this.getNext();
        boolean delayed = Delay.isDelayed(event);
        Location location = this.location.getSingle(event);
        if (location == null) {
            return next;
        }
        boolean unknownWorld = !location.isWorldLoaded();
        Entity[] entityArray = this.entities.getArray(event);
        if (entityArray.length == 0) {
            return next;
        }
        if (!delayed) {
            if (event instanceof PlayerRespawnEvent) {
                PlayerRespawnEvent playerRespawnEvent = (PlayerRespawnEvent)event;
                if (entityArray.length == 1 && entityArray[0].equals((Object)playerRespawnEvent.getPlayer())) {
                    if (unknownWorld) {
                        return next;
                    }
                    playerRespawnEvent.setRespawnLocation(location);
                    return next;
                }
            }
            if (event instanceof PlayerMoveEvent) {
                PlayerMoveEvent playerMoveEvent = (PlayerMoveEvent)event;
                if (entityArray.length == 1 && entityArray[0].equals((Object)playerMoveEvent.getPlayer())) {
                    if (unknownWorld) {
                        location = location.clone();
                        location.setWorld(playerMoveEvent.getFrom().getWorld());
                    }
                    playerMoveEvent.setTo(location);
                    return next;
                }
            }
        }
        if (unknownWorld) {
            if (entityArray.length == 1) {
                Entity entity = entityArray[0];
                if (entity == null) {
                    return next;
                }
                location = location.clone();
                location.setWorld(entity.getWorld());
            } else {
                return next;
            }
        }
        if (!this.async) {
            SkriptTeleportFlag[] teleportFlags = this.teleportFlags == null ? null : this.teleportFlags.getArray(event);
            for (Entity entity : entityArray) {
                this.teleport(entity, location, teleportFlags);
            }
            return next;
        }
        Location fixed = location;
        VariablesMap localVars = Variables.removeLocals(event);
        PaperLib.getChunkAtAsync(location).thenAccept(chunk -> {
            Delay.addDelayedEvent(event);
            SkriptTeleportFlag[] teleportFlags = this.teleportFlags == null ? null : this.teleportFlags.getArray(event);
            for (Entity entity : entityArray) {
                this.teleport(entity, fixed, teleportFlags);
            }
            if (localVars != null) {
                Variables.setLocalVariables(event, localVars);
            }
            Object timing = null;
            if (next != null) {
                Trigger trigger;
                if (SkriptTimings.enabled() && (trigger = this.getTrigger()) != null) {
                    timing = SkriptTimings.start(trigger.getDebugLabel());
                }
                TriggerItem.walk(next, event);
            }
            Variables.removeLocals(event);
            SkriptTimings.stop(timing);
        });
        return null;
    }

    @Override
    protected void execute(Event event) {
        assert (false);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug).append("teleport", this.entities, "to", this.location);
        if (this.teleportFlags != null) {
            builder.append("retaining", this.teleportFlags);
        }
        return builder.toString();
    }

    private void teleport(@NotNull Entity entity, @NotNull Location location, SkriptTeleportFlag ... skriptTeleportFlags) {
        if (location.getWorld() == null) {
            location = location.clone();
            location.setWorld(entity.getWorld());
        }
        if (!TELEPORT_FLAGS_SUPPORTED || skriptTeleportFlags == null) {
            entity.teleport(location);
            return;
        }
        Stream<TeleportFlag> teleportFlags = Arrays.stream(skriptTeleportFlags).flatMap(teleportFlag -> Stream.of(teleportFlag.getTeleportFlags())).filter(Objects::nonNull);
        entity.teleport(location, (TeleportFlag[])teleportFlags.toArray(TeleportFlag[]::new));
    }

    static {
        String extra = "";
        if (TELEPORT_FLAGS_SUPPORTED) {
            extra = " [[while] retaining %-teleportflags%]";
        }
        Skript.registerEffect(EffTeleport.class, "[:force] teleport %entities% (to|%direction%) %location%" + extra);
    }
}

