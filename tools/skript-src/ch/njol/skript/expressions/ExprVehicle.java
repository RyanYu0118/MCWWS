/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDismountEvent
 *  org.bukkit.event.entity.EntityMountEvent
 *  org.bukkit.event.vehicle.VehicleEnterEvent
 *  org.bukkit.event.vehicle.VehicleExitEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Kleenean;
import java.util.function.Predicate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Vehicle")
@Description(value={"The vehicle an entity is in, if any.", "This can actually be any entity, e.g. spider jockeys are skeletons that ride on a spider, so the spider is the 'vehicle' of the skeleton.", "See also: <a href='#ExprPassenger'>passenger</a>"})
@Example.Examples(value={@Example(value="set the vehicle of {game::players::*} to a saddled pig\ngive {game::players::*} a carrot on a stick\n"), @Example(value="on vehicle enter:\n\tvehicle is a horse\n\tadd 1 to {statistics::horseMounting::%uuid of player%}\n")})
@Since(value={"2.0"})
public class ExprVehicle
extends PropertyExpression<Entity, Entity> {
    static final /* synthetic */ boolean $assertionsDisabled;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(expressions[0]);
        return true;
    }

    protected Entity[] get(Event event, Entity[] source) {
        if (event instanceof EntityDismountEvent) {
            EntityDismountEvent entityDismountEvent = (EntityDismountEvent)event;
            if (this.getTime() != EventValues.TIME_FUTURE) {
                return this.get(source, e -> e.equals((Object)entityDismountEvent.getEntity()) ? entityDismountEvent.getDismounted() : e.getVehicle());
            }
        }
        if (event instanceof VehicleEnterEvent) {
            VehicleEnterEvent vehicleEnterEvent = (VehicleEnterEvent)event;
            if (this.getTime() != EventValues.TIME_PAST) {
                return this.get(source, e -> e.equals((Object)vehicleEnterEvent.getEntered()) ? vehicleEnterEvent.getVehicle() : e.getVehicle());
            }
        }
        if (event instanceof VehicleExitEvent) {
            VehicleExitEvent vehicleExitEvent = (VehicleExitEvent)event;
            if (this.getTime() != EventValues.TIME_FUTURE) {
                return this.get(source, e -> e.equals((Object)vehicleExitEvent.getExited()) ? vehicleExitEvent.getVehicle() : e.getVehicle());
            }
        }
        if (event instanceof EntityMountEvent) {
            EntityMountEvent entityMountEvent = (EntityMountEvent)event;
            if (this.getTime() != EventValues.TIME_PAST) {
                return this.get(source, e -> e.equals((Object)entityMountEvent.getEntity()) ? entityMountEvent.getMount() : e.getVehicle());
            }
        }
        return this.get(source, Entity::getVehicle);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            if (this.isDefault() && this.getParser().isCurrentEvent(VehicleExitEvent.class, EntityDismountEvent.class)) {
                Skript.error("Setting the vehicle during a dismount/exit vehicle event will create an infinite mounting loop.");
                return null;
            }
            return new Class[]{Entity.class, EntityData.class};
        }
        return super.acceptChange(mode);
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            VehicleEnterEvent vehicleEnterEvent;
            EntityMountEvent entityMountEvent;
            Predicate<Entity> predicate = Player.class::isInstance;
            if (event instanceof EntityMountEvent && predicate.test((entityMountEvent = (EntityMountEvent)event).getEntity())) {
                return;
            }
            if (event instanceof VehicleEnterEvent && predicate.test((vehicleEnterEvent = (VehicleEnterEvent)event).getEntered())) {
                return;
            }
            Entity[] passengers = (Entity[])this.getExpr().getArray(event);
            if (passengers.length == 0) {
                return;
            }
            if (!$assertionsDisabled && delta == null) {
                throw new AssertionError();
            }
            Object object = delta[0];
            if (object instanceof Entity) {
                Entity entity = (Entity)object;
                entity.eject();
                for (Entity passenger : passengers) {
                    if (event instanceof VehicleExitEvent && predicate.test(passenger) && passenger.equals((Object)((VehicleExitEvent)event).getExited()) || event instanceof EntityDismountEvent && predicate.test(passenger) && passenger.equals((Object)((EntityDismountEvent)event).getEntity())) continue;
                    if (!$assertionsDisabled && passenger == null) {
                        throw new AssertionError();
                    }
                    passenger.leaveVehicle();
                    entity.addPassenger(passenger);
                }
            } else if (object instanceof EntityData) {
                EntityData entityData = (EntityData)object;
                VehicleExitEvent vehicleExitEvent = event instanceof VehicleExitEvent ? (VehicleExitEvent)event : null;
                EntityDismountEvent entityDismountEvent = event instanceof EntityDismountEvent ? (EntityDismountEvent)event : null;
                for (Entity passenger : passengers) {
                    Object vehicle;
                    if (vehicleExitEvent != null && predicate.test(passenger) && passenger.equals((Object)vehicleExitEvent.getExited()) || entityDismountEvent != null && predicate.test(passenger) && passenger.equals((Object)entityDismountEvent.getEntity()) || (vehicle = entityData.spawn(passenger.getLocation())) == null) continue;
                    vehicle.addPassenger(passenger);
                }
            } else if (!$assertionsDisabled) {
                throw new AssertionError();
            }
        } else {
            super.change(event, delta, mode);
        }
    }

    @Override
    public boolean setTime(int time) {
        return super.setTime(time, this.getExpr(), VehicleEnterEvent.class, VehicleExitEvent.class, EntityMountEvent.class, EntityDismountEvent.class);
    }

    @Override
    public Class<? extends Entity> getReturnType() {
        return Entity.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "vehicle of " + this.getExpr().toString(event, debug);
    }

    static {
        boolean bl = $assertionsDisabled = !ExprVehicle.class.desiredAssertionStatus();
        if (Skript.classExists("org.bukkit.event.entity.EntityMountEvent")) {
            ExprVehicle.registerDefault(ExprVehicle.class, Entity.class, "vehicle[s]", "entities");
        }
    }
}

