/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.AbstractArrow
 *  org.bukkit.entity.Arrow
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Arrow Knockback Strength")
@Description(value={"An arrow's knockback strength."})
@Example(value="on shoot:\n\tevent-projectile is an arrow\n\tset arrow knockback strength of event-projectile to 10\n")
@Since(value={"2.5.1"})
public class ExprArrowKnockbackStrength
extends SimplePropertyExpression<Projectile, Long> {
    static final boolean abstractArrowExists = Skript.classExists("org.bukkit.entity.AbstractArrow");

    @Override
    @Nullable
    public Long convert(Projectile arrow) {
        if (abstractArrowExists) {
            return arrow instanceof AbstractArrow ? Long.valueOf(((AbstractArrow)arrow).getKnockbackStrength()) : null;
        }
        return arrow instanceof Arrow ? Long.valueOf(((Arrow)arrow).getKnockbackStrength()) : null;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET: 
            case ADD: 
            case RESET: 
            case REMOVE: {
                return CollectionUtils.array(Number.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        int strength = delta != null ? ((Number)delta[0]).intValue() : 0;
        switch (mode) {
            case REMOVE: {
                if (abstractArrowExists) {
                    for (Projectile entity : (Projectile[])this.getExpr().getArray(e)) {
                        if (!(entity instanceof AbstractArrow)) continue;
                        AbstractArrow abstractArrow = (AbstractArrow)entity;
                        int dmg = abstractArrow.getKnockbackStrength() - strength;
                        if (dmg < 0) {
                            dmg = 0;
                        }
                        abstractArrow.setKnockbackStrength(dmg);
                    }
                } else {
                    for (Projectile entity : (Projectile[])this.getExpr().getArray(e)) {
                        if (!(entity instanceof Arrow)) continue;
                        Arrow arrow = (Arrow)entity;
                        int dmg = arrow.getKnockbackStrength() - strength;
                        if (dmg < 0) {
                            return;
                        }
                        arrow.setKnockbackStrength(dmg);
                    }
                }
                break;
            }
            case ADD: {
                if (abstractArrowExists) {
                    for (Projectile entity : (Projectile[])this.getExpr().getArray(e)) {
                        if (!(entity instanceof AbstractArrow)) continue;
                        AbstractArrow abstractArrow = (AbstractArrow)entity;
                        int dmg = abstractArrow.getKnockbackStrength() + strength;
                        if (dmg < 0) {
                            return;
                        }
                        abstractArrow.setKnockbackStrength(dmg);
                    }
                } else {
                    for (Projectile entity : (Projectile[])this.getExpr().getArray(e)) {
                        if (!(entity instanceof Arrow)) continue;
                        Arrow arrow = (Arrow)entity;
                        int dmg = arrow.getKnockbackStrength() + strength;
                        if (dmg < 0) {
                            return;
                        }
                        arrow.setKnockbackStrength(dmg);
                    }
                }
                break;
            }
            case SET: 
            case RESET: {
                for (Projectile entity : (Projectile[])this.getExpr().getArray(e)) {
                    if (abstractArrowExists) {
                        if (!(entity instanceof AbstractArrow)) continue;
                        ((AbstractArrow)entity).setKnockbackStrength(strength);
                        continue;
                    }
                    if (!(entity instanceof Arrow)) continue;
                    ((Arrow)entity).setKnockbackStrength(strength);
                }
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    protected String getPropertyName() {
        return "projectile knockback strength";
    }

    static {
        ExprArrowKnockbackStrength.register(ExprArrowKnockbackStrength.class, Long.class, "arrow knockback strength", "projectiles");
    }
}

