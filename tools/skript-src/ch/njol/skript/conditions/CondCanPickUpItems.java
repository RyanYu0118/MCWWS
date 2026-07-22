/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.LivingEntity;

@Name(value="Can Pick Up Items")
@Description(value={"Whether living entities are able to pick up items off the ground or not."})
@Example.Examples(value={@Example(value="if player can pick items up:\n\tsend \"You can pick up items!\" to player\n"), @Example(value="on drop:\n\tif player can't pick up items:\n\t\tsend \"Be careful, you won't be able to pick that up!\" to player\n")})
@Since(value={"2.8.0"})
public class CondCanPickUpItems
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity livingEntity) {
        return livingEntity.getCanPickupItems();
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.CAN;
    }

    @Override
    protected String getPropertyName() {
        return "pick up items";
    }

    static {
        CondCanPickUpItems.register(CondCanPickUpItems.class, PropertyCondition.PropertyType.CAN, "pick([ ]up items| items up)", "livingentities");
    }
}

