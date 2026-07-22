/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Tameable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;

@Name(value="Is Tameable")
@Description(value={"Check if an entity is tameable."})
@Example(value="on damage:\n\tif victim is tameable:\n\t\tcancel event\n")
@Since(value={"2.5"})
public class CondIsTameable
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        return entity instanceof Tameable;
    }

    @Override
    protected String getPropertyName() {
        return "tameable";
    }

    static {
        CondIsTameable.register(CondIsTameable.class, "tameable", "livingentities");
    }
}

