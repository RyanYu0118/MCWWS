/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Damageable
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.bukkitutil.HealthUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Max Health")
@Description(value={"The maximum health of an entity, e.g. 10 for a player."})
@Example.Examples(value={@Example(value="on join:\n\tset the maximum health of the player to 100\n"), @Example(value="spawn a giant\nset the last spawned entity's max health to 1000\n")})
@Since(value={"2.0"})
@Events(value={"damage", "death"})
public class ExprMaxHealth
extends SimplePropertyExpression<LivingEntity, Number> {
    @Override
    public Number convert(LivingEntity e) {
        return HealthUtils.getMaxHealth((Damageable)e);
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "max health";
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode != Changer.ChangeMode.DELETE && mode != Changer.ChangeMode.REMOVE_ALL) {
            return new Class[]{Number.class};
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        double d = delta == null ? 0.0 : ((Number)delta[0]).doubleValue();
        block7: for (LivingEntity en : (LivingEntity[])this.getExpr().getArray(e)) {
            assert (en != null) : this.getExpr();
            switch (mode) {
                case SET: {
                    HealthUtils.setMaxHealth((Damageable)en, d);
                    continue block7;
                }
                case REMOVE: {
                    d = -d;
                }
                case ADD: {
                    HealthUtils.setMaxHealth((Damageable)en, HealthUtils.getMaxHealth((Damageable)en) + d);
                    continue block7;
                }
                case RESET: {
                    en.resetMaxHealth();
                    continue block7;
                }
                case DELETE: 
                case REMOVE_ALL: {
                    assert (false);
                    continue block7;
                }
            }
        }
    }

    static {
        ExprMaxHealth.register(ExprMaxHealth.class, Number.class, "max[imum] health", "livingentities");
    }
}

