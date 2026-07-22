/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Freeze Time")
@Description(value={"How much time an entity has been in powdered snow for."})
@Example(value="player's freeze time is less than 3 seconds:\n\tsend \"you're about to freeze!\" to the player\n")
@Since(value={"2.7"})
public class ExprFreezeTicks
extends SimplePropertyExpression<Entity, Timespan> {
    static final /* synthetic */ boolean $assertionsDisabled;

    @Override
    @Nullable
    public Timespan convert(Entity entity) {
        return new Timespan(Timespan.TimePeriod.TICK, entity.getFreezeTicks());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return mode != Changer.ChangeMode.REMOVE_ALL ? CollectionUtils.array(Timespan.class) : null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        int time = delta == null ? 0 : (int)((Timespan)delta[0]).getAs(Timespan.TimePeriod.TICK);
        switch (mode) {
            case ADD: {
                for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                    int newTime = entity.getFreezeTicks() + time;
                    this.setFreezeTicks(entity, newTime);
                }
                break;
            }
            case REMOVE: {
                for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                    int newTime = entity.getFreezeTicks() - time;
                    this.setFreezeTicks(entity, newTime);
                }
                break;
            }
            case SET: {
                for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                    this.setFreezeTicks(entity, time);
                }
                break;
            }
            case DELETE: 
            case RESET: {
                for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                    this.setFreezeTicks(entity, 0);
                }
                break;
            }
            default: {
                if (!$assertionsDisabled) {
                    throw new AssertionError();
                }
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
        return "freeze time";
    }

    private void setFreezeTicks(Entity entity, int ticks) {
        if (ticks < 0) {
            ticks = 0;
        }
        entity.setFreezeTicks(ticks);
    }

    static {
        boolean bl = $assertionsDisabled = !ExprFreezeTicks.class.desiredAssertionStatus();
        if (Skript.methodExists(Entity.class, "getFreezeTicks", new Class[0])) {
            ExprFreezeTicks.register(ExprFreezeTicks.class, Timespan.class, "freeze time", "entities");
        }
    }
}

