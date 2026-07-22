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

@Name(value="Is Frozen")
@Description(value={"Checks whether an entity is frozen."})
@Example(value="if player is frozen:\n\tkill player\n")
@Since(value={"2.7"})
public class CondIsFrozen
extends PropertyCondition<Entity> {
    @Override
    public boolean check(Entity entity) {
        return entity.isFrozen();
    }

    @Override
    protected String getPropertyName() {
        return "frozen";
    }

    static {
        if (Skript.methodExists(Entity.class, "isFrozen", new Class[0])) {
            CondIsFrozen.register(CondIsFrozen.class, "frozen", "entities");
        }
    }
}

