/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Panda
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Panda;

@Name(value="Panda Is Scared")
@Description(value={"Whether a panda is scared."})
@Example(value="if last spawned panda is scared:")
@Since(value={"2.11"})
public class CondPandaIsScared
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        Panda panda;
        return entity instanceof Panda && (panda = (Panda)entity).isScared();
    }

    @Override
    protected String getPropertyName() {
        return "scared";
    }

    static {
        CondPandaIsScared.register(CondPandaIsScared.class, "scared", "livingentities");
    }
}

