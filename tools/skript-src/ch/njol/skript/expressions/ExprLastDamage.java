/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Last Damage")
@Description(value={"The last damage that was done to an entity. Note that changing it doesn't deal more/less damage."})
@Example(value="set last damage of event-entity to 2")
@Since(value={"2.5.1"})
public class ExprLastDamage
extends SimplePropertyExpression<LivingEntity, Number> {
    @Override
    @Nullable
    public Number convert(LivingEntity livingEntity) {
        return livingEntity.getLastDamage() / 2.0;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case ADD: 
            case SET: 
            case REMOVE: {
                return CollectionUtils.array(Number.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (delta != null && delta[0] instanceof Number) {
            double damage = ((Number)delta[0]).doubleValue() * 2.0;
            switch (mode) {
                case SET: {
                    for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(e)) {
                        entity.setLastDamage(damage);
                    }
                    break;
                }
                case REMOVE: {
                    damage *= -1.0;
                }
                case ADD: {
                    for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(e)) {
                        entity.setLastDamage(damage + entity.getLastDamage());
                    }
                    break;
                }
                default: {
                    assert (false);
                    break;
                }
            }
        }
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "last damage";
    }

    static {
        ExprLastDamage.register(ExprLastDamage.class, Number.class, "last damage", "livingentities");
    }
}

