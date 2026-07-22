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

@Name(value="Entity is Wet")
@Description(value={"Checks whether an entity is wet or not (in water, rain or a bubble column)."})
@Example(value="if player is wet:")
@Since(value={"2.6.1"})
public class CondEntityIsWet
extends PropertyCondition<Entity> {
    @Override
    public boolean check(Entity entity) {
        return entity.isInWaterOrRainOrBubbleColumn();
    }

    @Override
    protected String getPropertyName() {
        return "wet";
    }

    static {
        if (Skript.methodExists(Entity.class, "isInWaterOrRainOrBubbleColumn", new Class[0])) {
            CondEntityIsWet.register(CondEntityIsWet.class, "wet", "entities");
        }
    }
}

