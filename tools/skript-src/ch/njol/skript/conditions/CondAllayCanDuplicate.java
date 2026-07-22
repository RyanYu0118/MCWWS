/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Allay
 *  org.bukkit.entity.LivingEntity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Allay;
import org.bukkit.entity.LivingEntity;

@Name(value="Allay Can Duplicate")
@Description(value={"Checks to see if an allay is able to duplicate naturally."})
@Example(value="if last spawned allay can duplicate:\n\tdisallow last spawned to duplicate\n")
@Since(value={"2.11"})
public class CondAllayCanDuplicate
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        boolean bl;
        if (entity instanceof Allay) {
            Allay allay = (Allay)entity;
            bl = allay.canDuplicate();
        } else {
            bl = false;
        }
        return bl;
    }

    @Override
    protected String getPropertyName() {
        return "duplicate";
    }

    static {
        CondAllayCanDuplicate.register(CondAllayCanDuplicate.class, PropertyCondition.PropertyType.CAN, "(duplicate|clone)", "livingentities");
    }
}

