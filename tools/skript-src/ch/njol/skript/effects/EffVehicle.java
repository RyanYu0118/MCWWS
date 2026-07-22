/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Vehicle")
@Description(value={"Makes an entity ride another entity, e.g. a minecart, a saddled pig, an arrow, etc."})
@Example.Examples(value={@Example(value="make the player ride a saddled pig"), @Example(value="make the attacker ride the victim")})
@Since(value={"2.0"})
public class EffVehicle
extends Effect {
    @Nullable
    private Expression<Entity> passengers;
    @Nullable
    private Expression<?> vehicles;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.passengers = matchedPattern == 2 ? null : exprs[0];
        this.vehicles = matchedPattern == 1 ? null : exprs[exprs.length - 1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        block12: {
            Object vehicleObject;
            Entity[] passengersArray;
            block11: {
                if (this.vehicles == null) {
                    assert (this.passengers != null);
                    for (Entity passenger : this.passengers.getArray(event)) {
                        passenger.leaveVehicle();
                    }
                    return;
                }
                if (this.passengers == null) {
                    for (Object vehicle : this.vehicles.getArray(event)) {
                        ((Entity)vehicle).eject();
                    }
                    return;
                }
                passengersArray = this.passengers.getArray(event);
                if (passengersArray.length == 0) {
                    return;
                }
                vehicleObject = this.vehicles.getSingle(event);
                if (!(vehicleObject instanceof Entity)) break block11;
                Entity vehicleEntity = (Entity)vehicleObject;
                for (Entity passenger : passengersArray) {
                    assert (passenger != null);
                    if (passenger == vehicleEntity) continue;
                    passenger.leaveVehicle();
                    vehicleEntity.addPassenger(passenger);
                }
                break block12;
            }
            if (!(vehicleObject instanceof EntityData)) break block12;
            EntityData vehicleData = (EntityData)vehicleObject;
            for (Entity passenger : passengersArray) {
                assert (passenger != null);
                Object vehicleEntity = vehicleData.spawn(passenger.getLocation());
                if (vehicleEntity == null) {
                    return;
                }
                passenger.leaveVehicle();
                vehicleEntity.addPassenger(passenger);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.vehicles == null) {
            assert (this.passengers != null);
            return "make " + this.passengers.toString(event, debug) + " dismount";
        }
        if (this.passengers == null) {
            return "eject passenger" + (this.vehicles.isSingle() ? "" : "s") + " of " + this.vehicles.toString(event, debug);
        }
        return "make " + this.passengers.toString(event, debug) + " ride " + this.vehicles.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffVehicle.class, "(make|let|force) %entities% [to] (ride|mount) [(in|on)] %entity/entitydata%", "(make|let|force) %entities% [to] (dismount|(dismount|leave) (from|of|) (any|the[ir]|his|her|) vehicle[s])", "(eject|dismount) (any|the|) passenger[s] (of|from) %entities%");
    }
}

