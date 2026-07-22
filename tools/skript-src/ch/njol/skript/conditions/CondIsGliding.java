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

@Name(value="Is Gliding")
@Description(value={"Checks whether a living entity is gliding."})
@Example(value="if player is gliding")
@Since(value={"2.7"})
public class CondIsGliding
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        return entity.isGliding();
    }

    @Override
    protected String getPropertyName() {
        return "gliding";
    }

    static {
        CondIsGliding.register(CondIsGliding.class, "gliding", "livingentities");
    }
}

