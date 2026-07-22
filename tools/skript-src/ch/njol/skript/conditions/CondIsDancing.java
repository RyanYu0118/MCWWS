/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Allay
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Parrot
 *  org.bukkit.entity.Piglin
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Allay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Piglin;

@Name(value="Is Dancing")
@Description(value={"Checks to see if an entity is dancing, such as allays, parrots, or piglins."})
@Example(value="if last spawned allay is dancing:\n\tbroadcast \"Dance Party!\"\n")
@Since(value={"2.11"})
public class CondIsDancing
extends PropertyCondition<LivingEntity> {
    private static final boolean SUPPORTS_PIGLINS = Skript.methodExists(Piglin.class, "isDancing", new Class[0]);

    @Override
    public boolean check(LivingEntity entity) {
        if (entity instanceof Allay) {
            Allay allay = (Allay)entity;
            return allay.isDancing();
        }
        if (entity instanceof Parrot) {
            Parrot parrot = (Parrot)entity;
            return parrot.isDancing();
        }
        if (SUPPORTS_PIGLINS && entity instanceof Piglin) {
            Piglin piglin = (Piglin)entity;
            return piglin.isDancing();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "dancing";
    }

    static {
        CondIsDancing.register(CondIsDancing.class, "dancing", "livingentities");
    }
}

