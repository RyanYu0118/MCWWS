/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Enderman
 *  org.bukkit.entity.LivingEntity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.LivingEntity;

@Name(value="Enderman Has Been Stared At")
@Description(value={"Checks to see if an enderman has been stared at.", "This will return true as long as the entity that stared at the enderman is still alive."})
@Example(value="if last spawned enderman has been stared at:")
@Since(value={"2.11"})
public class CondEndermanStaredAt
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        if (entity instanceof Enderman) {
            Enderman enderman = (Enderman)entity;
            return enderman.hasBeenStaredAt();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "stared at";
    }

    static {
        if (Skript.methodExists(Enderman.class, "hasBeenStaredAt", new Class[0])) {
            CondEndermanStaredAt.register(CondEndermanStaredAt.class, PropertyCondition.PropertyType.HAVE, "been stared at", "livingentities");
        }
    }
}

