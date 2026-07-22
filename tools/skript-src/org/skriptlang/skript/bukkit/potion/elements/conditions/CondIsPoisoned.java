/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.potion.PotionEffectType
 */
package org.skriptlang.skript.bukkit.potion.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Is Poisoned")
@Description(value={"Checks whether an entity is poisoned."})
@Example(value="if the player is poisoned:\n\tcure the player from poison\n\tmessage \"You have been cured!\" to the player\n")
@Since(value={"1.4.4"})
public class CondIsPoisoned
extends PropertyCondition<LivingEntity> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, CondIsPoisoned.infoBuilder(CondIsPoisoned.class, PropertyCondition.PropertyType.BE, "poisoned", "livingentities").supplier(CondIsPoisoned::new).build());
    }

    @Override
    public boolean check(LivingEntity entity) {
        return entity.hasPotionEffect(PotionEffectType.POISON);
    }

    @Override
    protected String getPropertyName() {
        return "poisoned";
    }
}

