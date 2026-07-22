/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="No Damage Time")
@Description(value={"The amount of time an entity is invulnerable to any damage."})
@Example.Examples(value={@Example(value="on damage:\n\tset victim's invulnerability time to 20 ticks #Victim will not take damage for the next second\n"), @Example(value="if the no damage timespan of {_entity} is 0 seconds:\n\tset the invincibility time span of {_entity} to 1 minute\n")})
@Since(value={"2.11"})
public class ExprNoDamageTime
extends SimplePropertyExpression<LivingEntity, Timespan> {
    @Override
    public Timespan convert(LivingEntity entity) {
        return new Timespan(Timespan.TimePeriod.TICK, entity.getNoDamageTicks());
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Timespan.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Object object;
        int providedTicks = 0;
        if (delta != null && (object = delta[0]) instanceof Timespan) {
            Timespan timespan = (Timespan)object;
            providedTicks = (int)timespan.getAs(Timespan.TimePeriod.TICK);
        }
        block5: for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(event)) {
            switch (mode) {
                case SET: 
                case DELETE: 
                case RESET: {
                    entity.setNoDamageTicks(providedTicks);
                    continue block5;
                }
                case ADD: {
                    int current = entity.getNoDamageTicks();
                    int value = Math2.fit(0, current + providedTicks, Integer.MAX_VALUE);
                    entity.setNoDamageTicks(value);
                    continue block5;
                }
                case REMOVE: {
                    int current = entity.getNoDamageTicks();
                    int value = Math2.fit(0, current - providedTicks, Integer.MAX_VALUE);
                    entity.setNoDamageTicks(value);
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
        return "no damage timespan";
    }

    static {
        ExprNoDamageTime.registerDefault(ExprNoDamageTime.class, Timespan.class, "(invulnerability|invincibility|no damage) time[[ ]span]", "livingentities");
    }
}

