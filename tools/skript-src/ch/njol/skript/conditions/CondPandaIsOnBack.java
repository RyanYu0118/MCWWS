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

@Name(value="Panda Is On Its Back")
@Description(value={"Whether a panda is on its back."})
@Example(value="if last spawned panda is on its back:\n\tmake last spawned panda get off its back\n")
@Since(value={"2.11"})
public class CondPandaIsOnBack
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        Panda panda;
        return entity instanceof Panda && (panda = (Panda)entity).isOnBack();
    }

    @Override
    protected String getPropertyName() {
        return "on their back";
    }

    static {
        CondPandaIsOnBack.register(CondPandaIsOnBack.class, "on (its|their) back[s]", "livingentities");
    }
}

