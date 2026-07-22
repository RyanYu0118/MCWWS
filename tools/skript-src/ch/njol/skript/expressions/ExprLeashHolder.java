/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@Name(value="Leash Holder")
@Description(value={"The leash holder of a living entity."})
@Example(value="set {_example} to the leash holder of the target mob")
@Since(value={"2.3"})
public class ExprLeashHolder
extends SimplePropertyExpression<LivingEntity, Entity> {
    @Override
    @Nullable
    public Entity convert(LivingEntity entity) {
        return entity.isLeashed() ? entity.getLeashHolder() : null;
    }

    @Override
    public Class<Entity> getReturnType() {
        return Entity.class;
    }

    @Override
    protected String getPropertyName() {
        return "leash holder";
    }

    static {
        ExprLeashHolder.register(ExprLeashHolder.class, Entity.class, "leash holder[s]", "livingentities");
    }
}

