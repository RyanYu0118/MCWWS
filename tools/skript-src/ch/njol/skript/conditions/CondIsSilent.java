/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Entity;

@Name(value="Is Silent")
@Description(value={"Checks whether an entity is silent i.e. its sounds are disabled."})
@Example(value="target entity is silent")
@Since(value={"2.5"})
public class CondIsSilent
extends PropertyCondition<Entity> {
    @Override
    public boolean check(Entity entity) {
        return entity.isSilent();
    }

    @Override
    protected String getPropertyName() {
        return "silent";
    }

    static {
        CondIsSilent.register(CondIsSilent.class, "silent", "entities");
    }
}

