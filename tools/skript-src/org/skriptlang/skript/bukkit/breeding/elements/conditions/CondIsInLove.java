/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Animals
 *  org.bukkit.entity.LivingEntity
 */
package org.skriptlang.skript.bukkit.breeding.elements.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Is In Love")
@Description(value={"Checks whether or not a living entity is in love."})
@Example(value="on spawn of living entity:\n\tif entity is in love:\n\t\tbroadcast \"That was quick!\"\n")
@Since(value={"2.10"})
public class CondIsInLove
extends PropertyCondition<LivingEntity> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, CondIsInLove.infoBuilder(CondIsInLove.class, PropertyCondition.PropertyType.BE, "in lov(e|ing) [state|mode]", "livingentities").supplier(CondIsInLove::new).build());
    }

    @Override
    public boolean check(LivingEntity entity) {
        if (entity instanceof Animals) {
            Animals animals = (Animals)entity;
            return animals.isLoveMode();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "in love";
    }
}

