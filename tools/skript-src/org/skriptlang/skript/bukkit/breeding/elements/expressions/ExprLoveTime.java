/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Animals
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.breeding.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Love Time")
@Description(value={"The amount of time the animals have been in love for. Using a value of 30 seconds is equivalent to using an item to breed them.", "Only works on animals that can be bred and returns '0 seconds' for animals that can't be bred."})
@Example(value="on right click:\n\tsend \"%event-entity% has been in love for %love time of event-entity% more than you!\" to player\n")
@Since(value={"2.10"})
public class ExprLoveTime
extends SimplePropertyExpression<LivingEntity, Timespan> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprLoveTime.infoBuilder(ExprLoveTime.class, Timespan.class, "love[d] time", "livingentities", false).supplier(ExprLoveTime::new)).build());
    }

    @Override
    @Nullable
    public Timespan convert(LivingEntity entity) {
        if (entity instanceof Animals) {
            Animals animal = (Animals)entity;
            return new Timespan(Timespan.TimePeriod.TICK, animal.getLoveModeTicks());
        }
        return new Timespan(0L);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET -> CollectionUtils.array(Timespan.class);
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Timespan[].class);
            case Changer.ChangeMode.RESET -> CollectionUtils.array(new Class[0]);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        int changeTicks = 0;
        if (delta != null) {
            for (Object object : delta) {
                changeTicks += (int)((Timespan)object).getAs(Timespan.TimePeriod.TICK);
            }
        }
        for (LivingEntity livingEntity : (LivingEntity[])this.getExpr().getArray(event)) {
            if (!(livingEntity instanceof Animals)) continue;
            Animals animal = (Animals)livingEntity;
            int loveTicks = animal.getLoveModeTicks();
            switch (mode) {
                case ADD: {
                    loveTicks += changeTicks;
                    break;
                }
                case REMOVE: {
                    loveTicks -= changeTicks;
                    break;
                }
                case SET: {
                    loveTicks = changeTicks;
                    break;
                }
                case RESET: {
                    loveTicks = 0;
                }
            }
            animal.setLoveModeTicks(Math.max(loveTicks, 0));
        }
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "love time";
    }
}

