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

@Name(value="Can Despawn")
@Description(value={"Check if an entity can despawn when the chunk they're located at is unloaded.", "More information on what and when entities despawn can be found at <a href=\"https://minecraft.wiki/w/Mob_spawning#Despawning\">reference</a>."})
@Example(value="if last spawned entity can despawn on chunk unload:\n\tmake last spawned entity not despawn on chunk unload\n")
@Since(value={"2.11"})
public class CondEntityUnload
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        return entity.getRemoveWhenFarAway();
    }

    @Override
    protected String getPropertyName() {
        return "despawn on chunk unload";
    }

    static {
        CondEntityUnload.register(CondEntityUnload.class, PropertyCondition.PropertyType.CAN, "despawn (on chunk unload|when far away)", "livingentities");
    }
}

