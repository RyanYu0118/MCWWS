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

@Name(value="Is Burning")
@Description(value={"Checks whether an entity is on fire, e.g. a zombie due to being in sunlight, or any entity after falling into lava."})
@Example(value="# increased attack against burning targets\nvictim is burning:\n\tincrease damage by 2\n")
@Since(value={"1.4.4"})
public class CondIsBurning
extends PropertyCondition<Entity> {
    @Override
    public boolean check(Entity entity) {
        return entity.getFireTicks() > 0;
    }

    @Override
    protected String getPropertyName() {
        return "burning";
    }

    static {
        CondIsBurning.register(CondIsBurning.class, "(burning|ignited|on fire)", "entities");
    }
}

