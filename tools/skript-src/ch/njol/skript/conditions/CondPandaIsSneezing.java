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

@Name(value="Panda Is Sneezing")
@Description(value={"Whether a panda is sneezing."})
@Example(value="if last spawned panda is sneezing:\n\tmake last spawned panda stop sneezing\n")
@Since(value={"2.11"})
public class CondPandaIsSneezing
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        Panda panda;
        return entity instanceof Panda && (panda = (Panda)entity).isSneezing();
    }

    @Override
    protected String getPropertyName() {
        return "sneezing";
    }

    static {
        CondPandaIsSneezing.register(CondPandaIsSneezing.class, "sneezing", "livingentities");
    }
}

