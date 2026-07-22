/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Damageable
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.bukkitutil.HealthUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Last Damage Cause")
@Description(value={"Cause of last damage done to an entity"})
@Example(value="set last damage cause of event-entity to fire tick")
@Since(value={"2.2-Fixes-V10"})
public class ExprLastDamageCause
extends PropertyExpression<LivingEntity, EntityDamageEvent.DamageCause> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.setExpr(exprs[0]);
        return true;
    }

    protected EntityDamageEvent.DamageCause[] get(Event e, LivingEntity[] source) {
        return this.get(source, entity -> {
            EntityDamageEvent dmgEvt = entity.getLastDamageCause();
            if (dmgEvt == null) {
                return EntityDamageEvent.DamageCause.CUSTOM;
            }
            return dmgEvt.getCause();
        });
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the damage cause " + this.getExpr().toString(e, debug);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.REMOVE_ALL) {
            return null;
        }
        return CollectionUtils.array(EntityDamageEvent.DamageCause.class);
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        EntityDamageEvent.DamageCause d;
        EntityDamageEvent.DamageCause damageCause = d = delta == null ? EntityDamageEvent.DamageCause.CUSTOM : (EntityDamageEvent.DamageCause)delta[0];
        assert (d != null);
        switch (mode) {
            case DELETE: 
            case RESET: {
                for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(e)) {
                    assert (entity != null) : this.getExpr();
                    HealthUtils.setDamageCause((Damageable)entity, EntityDamageEvent.DamageCause.CUSTOM);
                }
                break;
            }
            case SET: {
                for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(e)) {
                    assert (entity != null) : this.getExpr();
                    HealthUtils.setDamageCause((Damageable)entity, d);
                }
                break;
            }
            case REMOVE_ALL: {
                assert (false);
                break;
            }
        }
    }

    @Override
    public Class<EntityDamageEvent.DamageCause> getReturnType() {
        return EntityDamageEvent.DamageCause.class;
    }

    static {
        ExprLastDamageCause.register(ExprLastDamageCause.class, EntityDamageEvent.DamageCause.class, "last damage (cause|reason|type)", "livingentities");
    }
}

