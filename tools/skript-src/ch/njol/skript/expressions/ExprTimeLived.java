/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Time Lived of Entity")
@Description(value={"Returns the total amount of time the entity has lived.\nNote: This does not reset when a player dies.\n"})
@Example.Examples(value={@Example(value="clear all entities where [input's time lived > 1 hour]"), @Example(value="on right click on entity:\n\tsend \"%entity% has lived for %time lived of entity%\" to player\n")})
@Since(value={"2.13"})
public class ExprTimeLived
extends SimplePropertyExpression<Entity, Timespan> {
    @Override
    @Nullable
    public Timespan convert(Entity entity) {
        return new Timespan(Timespan.TimePeriod.TICK, entity.getTicksLived());
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.SET, Changer.ChangeMode.RESET -> CollectionUtils.array(Timespan.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Object object;
        long newTicks = 1L;
        if (delta != null && (object = delta[0]) instanceof Timespan) {
            Timespan timespan = (Timespan)object;
            newTicks = (int)timespan.get(Timespan.TimePeriod.TICK);
        }
        for (Entity entity : (Entity[])this.getExpr().getArray(event)) {
            int currentTicks = entity.getTicksLived();
            long valueToSet = switch (mode) {
                case Changer.ChangeMode.ADD -> (long)currentTicks + newTicks;
                case Changer.ChangeMode.REMOVE -> (long)currentTicks - newTicks;
                case Changer.ChangeMode.SET, Changer.ChangeMode.RESET -> newTicks;
                default -> currentTicks;
            };
            entity.setTicksLived((int)Math2.fit(1L, valueToSet, Integer.MAX_VALUE));
        }
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "time lived";
    }

    static {
        ExprTimeLived.register(ExprTimeLived.class, Timespan.class, "time (alive|lived)", "entities");
    }
}

