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
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.potion.util.SkriptPotionEffect;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Potion Effect - Amplifier")
@Description(value={"An expression to obtain the amplifier of a potion effect."})
@Example.Examples(value={@Example(value="set the amplifier of {_potion} to 10"), @Example(value="add 10 to the amplifier of the player's speed effect")})
@Since(value={"2.7", "2.14 (support for potion effect objects, changing)"})
public class ExprPotionAmplifier
extends SimplePropertyExpression<SkriptPotionEffect, Integer> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprPotionAmplifier.infoBuilder(ExprPotionAmplifier.class, Integer.class, "([potion] amplifier|potion tier|potion level)[s]", "skriptpotioneffects", true).supplier(ExprPotionAmplifier::new)).build());
    }

    @Override
    public Integer convert(SkriptPotionEffect potionEffect) {
        return potionEffect.amplifier() + 1;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (!SkriptPotionEffect.isChangeable(this.getExpr())) {
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.SET, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Integer.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        assert (delta != null);
        int change = (Integer)delta[0];
        if (mode == Changer.ChangeMode.REMOVE) {
            change = -change;
        }
        for (SkriptPotionEffect potionEffect : (SkriptPotionEffect[])this.getExpr().getArray(event)) {
            int base = mode == Changer.ChangeMode.SET ? -1 : potionEffect.amplifier();
            potionEffect.amplifier(change + base);
        }
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return "amplifier";
    }
}

