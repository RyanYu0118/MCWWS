/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Ghast
 *  org.bukkit.entity.LivingEntity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;

@Name(value="Is Charging Fireball")
@Description(value={"Check whether a ghast is charging a fireball."})
@Example(value="if last spawned ghast is charging fireball:\n\tkill last spawned ghast\n")
@Since(value={"2.11"})
public class CondIsChargingFireball
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        if (entity instanceof Ghast) {
            Ghast ghast = (Ghast)entity;
            return ghast.isCharging();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "charging fireball";
    }

    static {
        CondIsChargingFireball.register(CondIsChargingFireball.class, "charging [a] fireball", "livingentities");
    }
}

