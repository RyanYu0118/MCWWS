/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
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
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Pickup Delay")
@Description(value={"The amount of time before a dropped item can be picked up by an entity."})
@Example.Examples(value={@Example(value="drop diamond sword at {_location} without velocity"), @Example(value="set pickup delay of last dropped item to 5 seconds")})
@Since(value={"2.7"})
public class ExprPickupDelay
extends SimplePropertyExpression<Entity, Timespan> {
    @Override
    @Nullable
    public Timespan convert(Entity entity) {
        if (!(entity instanceof Item)) {
            return null;
        }
        return new Timespan(Timespan.TimePeriod.TICK, ((Item)entity).getPickupDelay());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET: 
            case ADD: 
            case RESET: 
            case DELETE: 
            case REMOVE: {
                return CollectionUtils.array(Timespan.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Entity[] entities = (Entity[])this.getExpr().getArray(event);
        int change = delta == null ? 0 : (int)((Timespan)delta[0]).getAs(Timespan.TimePeriod.TICK);
        switch (mode) {
            case REMOVE: {
                change = -change;
            }
            case ADD: {
                for (Entity entity : entities) {
                    if (!(entity instanceof Item)) continue;
                    Item item = (Item)entity;
                    item.setPickupDelay(item.getPickupDelay() + change);
                }
                break;
            }
            case SET: 
            case RESET: 
            case DELETE: {
                for (Entity entity : entities) {
                    if (!(entity instanceof Item)) continue;
                    ((Item)entity).setPickupDelay(change);
                }
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "pickup delay";
    }

    static {
        ExprPickupDelay.register(ExprPickupDelay.class, Timespan.class, "pick[ ]up delay", "entities");
    }
}

