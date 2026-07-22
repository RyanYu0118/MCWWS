/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Tameable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;

@Name(value="Is Tamed")
@Description(value={"Check if a tameable entity is tamed (horse, parrot, cat, etc.)."})
@Example.Examples(value={@Example(value="send true if {_horse} is tamed"), @Example(value="tame {_horse} if {_horse} is untamed")})
@Since(value={"2.10"})
public class CondIsTamed
extends PropertyCondition<Entity> {
    @Override
    public boolean check(Entity entity) {
        Tameable tameable;
        return entity instanceof Tameable && (tameable = (Tameable)entity).isTamed();
    }

    @Override
    protected String getPropertyName() {
        return "tamed";
    }

    static {
        CondIsTamed.register(CondIsTamed.class, "(tamed|domesticated)", "entities");
    }
}

