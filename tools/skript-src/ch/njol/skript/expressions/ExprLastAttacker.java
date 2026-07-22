/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprAttacker;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Last Attacker")
@Description(value={"The last block or entity that attacked an entity."})
@Example(value="send \"%last attacker of event-entity%\"")
@Since(value={"2.5.1"})
public class ExprLastAttacker
extends SimplePropertyExpression<Entity, Entity> {
    @Override
    @Nullable
    public Entity convert(Entity entity) {
        return ExprAttacker.getAttacker((Event)entity.getLastDamageCause());
    }

    @Override
    public Class<? extends Entity> getReturnType() {
        return Entity.class;
    }

    @Override
    protected String getPropertyName() {
        return "last attacker";
    }

    static {
        ExprLastAttacker.register(ExprLastAttacker.class, Entity.class, "last attacker", "entity");
    }
}

