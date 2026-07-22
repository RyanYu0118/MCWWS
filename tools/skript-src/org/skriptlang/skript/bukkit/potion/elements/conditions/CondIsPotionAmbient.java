/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.bukkit.potion.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.skriptlang.skript.bukkit.potion.util.SkriptPotionEffect;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Potion Effect - Is Ambient")
@Description(value={"Checks whether a potion effect is ambient.", "That is, whether the potion effect produces more, translucent, particles."})
@Example(value="on entity potion effect modification:\n\tif the potion effect is ambient:\n\t\tmessage \"It's particle time!\"\n")
@Since(value={"2.14"})
public class CondIsPotionAmbient
extends PropertyCondition<SkriptPotionEffect> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, CondIsPotionAmbient.infoBuilder(CondIsPotionAmbient.class, PropertyCondition.PropertyType.BE, "ambient", "skriptpotioneffects").supplier(CondIsPotionAmbient::new).build());
    }

    @Override
    public boolean check(SkriptPotionEffect potionEffect) {
        return potionEffect.ambient();
    }

    @Override
    protected String getPropertyName() {
        return "ambient";
    }
}

