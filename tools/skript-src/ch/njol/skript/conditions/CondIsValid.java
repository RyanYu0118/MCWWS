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
import org.skriptlang.skript.util.Validated;

@Name(value="Is Valid")
@Description(value={"Checks whether something (an entity, a script, a config, etc.) is valid.", "An invalid entity may have died or de-spawned for some other reason.", "An invalid script reference may have been reloaded, moved or disabled since."})
@Example(value="if event-entity is valid")
@Since(value={"2.7, 2.10 (Scripts & Configs)"})
public class CondIsValid
extends PropertyCondition<Object> {
    @Override
    public boolean check(Object value) {
        if (value instanceof Entity) {
            Entity entity = (Entity)value;
            return entity.isValid();
        }
        if (value instanceof Validated) {
            Validated validated = (Validated)value;
            return validated.valid();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "valid";
    }

    static {
        CondIsValid.register(CondIsValid.class, "valid", "entities/scripts");
    }
}

