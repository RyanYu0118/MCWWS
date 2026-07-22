/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.event.vehicle.VehicleEnterEvent
 *  org.bukkit.event.vehicle.VehicleExitEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.PassengerUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.Arrays;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converter;

@Name(value="Passenger")
@Description(value={"The passenger of a vehicle, or the rider of a mob.", "For 1.11.2 and above, it returns a list of passengers and you can use all changers in it.", "See also: <a href='#ExprVehicle'>vehicle</a>"})
@Example(value="passengers of the minecart contains a creeper or a cow\nthe boat's passenger contains a pig\nadd a cow and a zombie to passengers of last spawned boat\nset passengers of player's vehicle to a pig and a horse\nremove all pigs from player's vehicle\nclear passengers of boat\n")
@Since(value={"2.0, 2.2-dev26 (Multiple passengers for 1.11.2+)"})
public class ExprPassenger
extends SimpleExpression<Entity> {
    private Expression<Entity> vehicle;

    @Nullable
    protected Entity[] get(final Event e) {
        Entity[] source = this.vehicle.getAll(e);
        Converter<Entity, Entity[]> conv = new Converter<Entity, Entity[]>(){
            final /* synthetic */ ExprPassenger this$0;
            {
                this.this$0 = this$0;
            }

            @Override
            @Nullable
            public Entity[] convert(Entity v) {
                if (this.this$0.getTime() >= 0 && e instanceof VehicleEnterEvent && v.equals((Object)((VehicleEnterEvent)e).getVehicle()) && !Delay.isDelayed(e)) {
                    return new Entity[]{((VehicleEnterEvent)e).getEntered()};
                }
                if (this.this$0.getTime() >= 0 && e instanceof VehicleExitEvent && v.equals((Object)((VehicleExitEvent)e).getVehicle()) && !Delay.isDelayed(e)) {
                    return new Entity[]{((VehicleExitEvent)e).getExited()};
                }
                return PassengerUtils.getPassenger(v);
            }
        };
        ArrayList<Entity> entities = new ArrayList<Entity>();
        for (Entity v : source) {
            Entity[] array;
            if (v == null || (array = (Entity[])conv.convert(v)) == null || array.length <= 0) continue;
            entities.addAll(Arrays.asList(array));
        }
        return entities.toArray(new Entity[entities.size()]);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.vehicle = exprs[0];
        return true;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (!this.isSingle()) {
            return new Class[]{Entity[].class, EntityData[].class};
        }
        if (mode == Changer.ChangeMode.SET) {
            return new Class[]{Entity.class, EntityData.class};
        }
        return super.acceptChange(mode);
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Entity[] vehicles = this.vehicle.getArray(e);
        if (!this.isSingle() || mode == Changer.ChangeMode.SET) {
            block6: for (Entity vehicle : vehicles) {
                if (vehicle == null) continue;
                switch (mode) {
                    case SET: {
                        vehicle.eject();
                    }
                    case ADD: {
                        if (delta == null || delta.length == 0) {
                            return;
                        }
                        for (Object obj : delta) {
                            if (obj == null) continue;
                            Entity passenger = obj instanceof Entity ? (Entity)obj : ((EntityData)obj).spawn(vehicle.getLocation());
                            PassengerUtils.addPassenger(vehicle, passenger);
                        }
                        continue block6;
                    }
                    case REMOVE_ALL: 
                    case REMOVE: {
                        if (delta == null || delta.length == 0) {
                            return;
                        }
                        for (Object obj : delta) {
                            if (obj == null) continue;
                            if (obj instanceof Entity) {
                                PassengerUtils.removePassenger(vehicle, (Entity)obj);
                                continue;
                            }
                            for (Entity passenger : PassengerUtils.getPassenger(vehicle)) {
                                if (passenger == null || !((EntityData)obj).isInstance(passenger)) continue;
                                PassengerUtils.removePassenger(vehicle, passenger);
                            }
                        }
                        continue block6;
                    }
                    case RESET: 
                    case DELETE: {
                        vehicle.eject();
                    }
                }
            }
        } else {
            super.change(e, delta, mode);
        }
    }

    @Override
    public Class<? extends Entity> getReturnType() {
        return Entity.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the passenger of " + this.vehicle.toString(e, debug);
    }

    @Override
    public boolean isSingle() {
        return !PassengerUtils.hasMultiplePassenger() ? this.vehicle.isSingle() : false;
    }

    @Override
    public boolean setTime(int time) {
        return super.setTime(time, this.vehicle, VehicleEnterEvent.class, VehicleExitEvent.class);
    }

    static {
        Skript.registerExpression(ExprPassenger.class, Entity.class, ExpressionType.PROPERTY, "[the] passenger[s] of %entities%", "%entities%'[s] passenger[s]");
    }
}

