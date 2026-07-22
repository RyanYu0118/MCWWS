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

@Name(value="Is Swimming")
@Description(value={"Checks whether a living entity is swimming."})
@Example(value="player is swimming")
@Since(value={"2.3"})
public class CondIsSwimming
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        return entity.isSwimming();
    }

    @Override
    protected String getPropertyName() {
        return "swimming";
    }

    static {
        CondIsSwimming.register(CondIsSwimming.class, "swimming", "livingentities");
    }
}

