/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.damage.DamageSource
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.damagesource.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.damage.DamageSource;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Damage Source - Food Exhaustion")
@Description(value={"The amount of hunger exhaustion caused by a damage source."})
@Example(value="on damage:\n\tif the food exhaustion of event-damage source is 10:\n")
@Since(value={"2.12"})
public class ExprFoodExhaustion
extends SimplePropertyExpression<DamageSource, Float> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprFoodExhaustion.infoBuilder(ExprFoodExhaustion.class, Float.class, "food exhaustion", "damagesources", true).supplier(ExprFoodExhaustion::new)).build());
    }

    @Override
    @Nullable
    public Float convert(DamageSource damageSource) {
        return Float.valueOf(damageSource.getFoodExhaustion());
    }

    @Override
    public Class<Float> getReturnType() {
        return Float.class;
    }

    @Override
    protected String getPropertyName() {
        return "food exhaustion";
    }
}

