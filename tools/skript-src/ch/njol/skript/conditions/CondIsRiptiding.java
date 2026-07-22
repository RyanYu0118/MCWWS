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

@Name(value="Is Riptiding")
@Description(value={"Checks to see if an entity is currently using the Riptide enchantment."})
@Example(value="target entity is riptiding")
@Since(value={"2.5"})
public class CondIsRiptiding
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        return entity.isRiptiding();
    }

    @Override
    protected String getPropertyName() {
        return "riptiding";
    }

    static {
        CondIsRiptiding.register(CondIsRiptiding.class, "riptiding", "livingentities");
    }
}

