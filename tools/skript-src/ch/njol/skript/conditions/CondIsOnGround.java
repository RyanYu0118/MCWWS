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

@Name(value="Is on Ground")
@Description(value={"Checks whether an entity is on ground."})
@Example(value="player is not on ground")
@Since(value={"2.2-dev26"})
public class CondIsOnGround
extends PropertyCondition<Entity> {
    @Override
    public boolean check(Entity entity) {
        return entity.isOnGround();
    }

    @Override
    protected String getPropertyName() {
        return "on ground";
    }

    static {
        PropertyCondition.register(CondIsOnGround.class, "on [the] ground", "entities");
    }
}

