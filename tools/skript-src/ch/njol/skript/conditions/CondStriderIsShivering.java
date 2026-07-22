/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Strider
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Strider;

@Name(value="Strider Is Shivering")
@Description(value={"Whether a strider is shivering."})
@Example(value="if last spawned strider is shivering:\n\tmake last spawned strider stop shivering\n")
@Since(value={"2.12"})
public class CondStriderIsShivering
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        Strider strider;
        return entity instanceof Strider && (strider = (Strider)entity).isShivering();
    }

    @Override
    protected String getPropertyName() {
        return "shivering";
    }

    static {
        CondStriderIsShivering.register(CondStriderIsShivering.class, "shivering", "livingentities");
    }
}

