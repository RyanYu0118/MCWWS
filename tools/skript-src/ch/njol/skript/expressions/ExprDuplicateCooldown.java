/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Allay
 *  org.bukkit.entity.LivingEntity
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
import org.bukkit.entity.Allay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Allay Duplication Cooldown")
@Description(value={"The cooldown time until an allay can duplicate again naturally.", "Resetting the cooldown time will set the cooldown time to the same amount of time after an allay has duplicated."})
@Example.Examples(value={@Example(value="set {_time} to the duplicate cooldown of last spawned allay"), @Example(value="add 5 seconds to the duplication cool down time of last spawned allay"), @Example(value="remove 3 seconds from the duplicating cooldown time of last spawned allay"), @Example(value="clear the clone cool down of last spawned allay"), @Example(value="reset the cloning cool down time of last spawned allay")})
@Since(value={"2.11"})
public class ExprDuplicateCooldown
extends SimplePropertyExpression<LivingEntity, Timespan> {
    @Override
    @Nullable
    public Timespan convert(LivingEntity entity) {
        if (entity instanceof Allay) {
            Allay allay = (Allay)entity;
            return new Timespan(Timespan.TimePeriod.TICK, allay.getDuplicationCooldown());
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(Timespan.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        long ticks = delta == null ? 0L : ((Timespan)delta[0]).getAs(Timespan.TimePeriod.TICK);
        ticks = Math2.fit(0L, ticks, Long.MAX_VALUE);
        block6: for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(event)) {
            if (!(entity instanceof Allay)) continue;
            Allay allay = (Allay)entity;
            switch (mode) {
                case SET: 
                case DELETE: {
                    allay.setDuplicationCooldown(ticks);
                    continue block6;
                }
                case ADD: {
                    long current = allay.getDuplicationCooldown();
                    long value = Math2.fit(0L, current + ticks, Long.MAX_VALUE);
                    allay.setDuplicationCooldown(value);
                    continue block6;
                }
                case REMOVE: {
                    long current = allay.getDuplicationCooldown();
                    long value = Math2.fit(0L, current - ticks, Long.MAX_VALUE);
                    allay.setDuplicationCooldown(value);
                    continue block6;
                }
                case RESET: {
                    allay.resetDuplicationCooldown();
                }
            }
        }
    }

    @Override
    public Class<Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "duplicate cooldown time";
    }

    static {
        ExprDuplicateCooldown.registerDefault(ExprDuplicateCooldown.class, Timespan.class, "(duplicat(e|ing|ion)|clon(e|ing)) cool[ ]down [time]", "livingentities");
    }
}

