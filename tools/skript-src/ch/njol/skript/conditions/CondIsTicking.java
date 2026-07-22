/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Entity;

@Name(value="Is Ticking")
@Description(value={"Checks if an entity is ticking."})
@Example(value="send true if target is ticking")
@Since(value={"2.10"})
public class CondIsTicking
extends PropertyCondition<Entity> {
    @Override
    public boolean check(Entity entity) {
        return entity.isTicking();
    }

    @Override
    protected String getPropertyName() {
        return "ticking";
    }

    static {
        if (Skript.methodExists(Entity.class, "isTicking", new Class[0])) {
            CondIsTicking.register(CondIsTicking.class, "ticking", "entities");
        }
    }
}

