/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Breedable
 *  org.bukkit.entity.LivingEntity
 */
package org.skriptlang.skript.bukkit.breeding.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Breedable;
import org.bukkit.entity.LivingEntity;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Can Breed")
@Description(value={"Checks whether or not a living entity can be bred."})
@Example(value="on right click on living entity:\n\tevent-entity can't breed\n\tsend \"Turns out %event-entity% is not breedable. Must be a Skript user!\" to player\n")
@Since(value={"2.10"})
public class CondCanBreed
extends PropertyCondition<LivingEntity> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, CondCanBreed.infoBuilder(CondCanBreed.class, PropertyCondition.PropertyType.CAN, "(breed|be bred)", "livingentities").supplier(CondCanBreed::new).build());
    }

    @Override
    public boolean check(LivingEntity entity) {
        Breedable breedable;
        return entity instanceof Breedable && (breedable = (Breedable)entity).canBreed();
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.CAN;
    }

    @Override
    protected String getPropertyName() {
        return "breed";
    }
}

