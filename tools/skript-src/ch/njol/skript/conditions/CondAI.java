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

@Name(value="Has AI")
@Description(value={"Checks whether an entity has AI."})
@Example(value="target entity has ai")
@Since(value={"2.5"})
public class CondAI
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        return entity.hasAI();
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.HAVE;
    }

    @Override
    protected String getPropertyName() {
        return "artificial intelligence";
    }

    static {
        CondAI.register(CondAI.class, PropertyCondition.PropertyType.HAVE, "(ai|artificial intelligence)", "livingentities");
    }
}

