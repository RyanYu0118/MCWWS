/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Camel
 *  org.bukkit.entity.LivingEntity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.entity.Camel;
import org.bukkit.entity.LivingEntity;

@Name(value="Camel Is Dashing")
@Description(value={"Checks whether a camel is currently using its dash ability."})
@Example(value="if last spawned camel is dashing:\n\tkill last spawned camel\n")
@Since(value={"2.11"})
public class CondIsDashing
extends PropertyCondition<LivingEntity> {
    @Override
    public boolean check(LivingEntity entity) {
        if (entity instanceof Camel) {
            Camel camel = (Camel)entity;
            return camel.isDashing();
        }
        return false;
    }

    @Override
    protected String getPropertyName() {
        return "dashing";
    }

    static {
        CondIsDashing.register(CondIsDashing.class, "dashing", "livingentities");
    }
}

