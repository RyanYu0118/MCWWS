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

@Name(value="Is Leashed")
@Description(value={"Checks to see if an entity is currently leashed."})
@Example(value="target entity is leashed")
@Since(value={"2.5"})
public class CondLeashed
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        return entity.isLeashed();
    }

    @Override
    protected String getPropertyName() {
        return "leashed";
    }

    static {
        CondLeashed.register(CondLeashed.class, PropertyCondition.PropertyType.BE, "leashed", "livingentities");
    }
}

