/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.entity.Shearable
 *  org.bukkit.entity.Cow
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Sheep
 *  org.bukkit.entity.Snowman
 *  org.bukkit.event.entity.CreatureSpawnEvent$SpawnReason
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import io.papermc.paper.entity.Shearable;
import org.bukkit.entity.Cow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Snowman;
import org.bukkit.event.entity.CreatureSpawnEvent;

@Name(value="Entity Is Sheared")
@Description(value={"Checks whether entities are sheared."})
@Example(value="if targeted entity of player is sheared:\n\tsend \"This entity has nothing left to shear!\" to player\n")
@Since(value={"2.8.0"})
public class CondIsSheared
extends PropertyCondition<LivingEntity> {
    private static final boolean INTERFACE_METHOD = Skript.classExists("io.papermc.paper.entity.Shearable");

    @Override
    public boolean check(LivingEntity entity) {
        if (entity instanceof Cow) {
            return entity.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.SHEARED;
        }
        if (INTERFACE_METHOD) {
            if (!(entity instanceof Shearable)) {
                return false;
            }
            Shearable shearable = (Shearable)entity;
            return !shearable.readyToBeSheared();
        }
        if (entity instanceof Sheep) {
            Sheep sheep = (Sheep)entity;
            return sheep.isSheared();
        }
        if (entity instanceof Snowman) {
            Snowman snowman = (Snowman)entity;
            return snowman.isDerp();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "sheared";
    }

    static {
        CondIsSheared.register(CondIsSheared.class, "(sheared|shorn)", "livingentities");
    }
}

