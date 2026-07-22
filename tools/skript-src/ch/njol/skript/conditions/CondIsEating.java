/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.AbstractHorse
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Panda
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Panda;

@Name(value="Is Eating")
@Description(value={"Whether a panda or horse type (horse, camel, donkey, llama, mule) is eating."})
@Example(value="if last spawned panda is eating:\n\tforce last spawned panda to stop eating\n")
@Since(value={"2.11"})
public class CondIsEating
extends PropertyCondition<LivingEntity> {
    private static final boolean SUPPORTS_HORSES = Skript.methodExists(AbstractHorse.class, "isEating", new Class[0]);

    @Override
    public boolean check(LivingEntity entity) {
        if (entity instanceof Panda) {
            Panda panda = (Panda)entity;
            return panda.isEating();
        }
        if (SUPPORTS_HORSES && entity instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse)entity;
            return horse.isEating();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "eating";
    }

    static {
        CondIsEating.register(CondIsEating.class, "eating", "livingentities");
    }
}

