/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.potion.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.potion.util.SkriptPotionEffect;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Potion Effect - Duration")
@Description(value={"An expression to obtain the duration of a potion effect."})
@Example.Examples(value={@Example(value="set the duration of {_potion} to 10 seconds"), @Example(value="add 10 seconds to the duration of the player's speed effect")})
@Since(value={"2.14"})
public class ExprPotionDuration
extends SimplePropertyExpression<SkriptPotionEffect, Timespan> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprPotionDuration.infoBuilder(ExprPotionDuration.class, Timespan.class, "([potion] duration|potion length)[s]", "skriptpotioneffects", true).supplier(ExprPotionDuration::new)).build());
    }

    @Override
    public Timespan convert(SkriptPotionEffect potionEffect) {
        if (potionEffect.infinite()) {
            return Timespan.infinite();
        }
        return new Timespan(Timespan.TimePeriod.TICK, potionEffect.duration());
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (!SkriptPotionEffect.isChangeable(this.getExpr())) {
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.SET, Changer.ChangeMode.REMOVE, Changer.ChangeMode.RESET -> CollectionUtils.array(Timespan.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Timespan change = delta != null ? (Timespan)delta[0] : new Timespan(Timespan.TimePeriod.TICK, 600L);
        for (SkriptPotionEffect potionEffect : (SkriptPotionEffect[])this.getExpr().getArray(event)) {
            ExprPotionDuration.changeSafe(potionEffect, change, mode);
        }
    }

    static void changeSafe(SkriptPotionEffect potionEffect, Timespan change, Changer.ChangeMode mode) {
        Timespan duration;
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) {
            duration = change;
        } else {
            if (potionEffect.infinite()) {
                return;
            }
            duration = new Timespan(Timespan.TimePeriod.TICK, potionEffect.duration());
            duration = mode == Changer.ChangeMode.ADD ? duration.add(change) : duration.subtract(change);
        }
        if (duration.isInfinite()) {
            potionEffect.infinite(true);
        } else {
            potionEffect.duration((int)Math2.fit(0L, duration.getAs(Timespan.TimePeriod.TICK), Integer.MAX_VALUE));
        }
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return "duration";
    }
}

