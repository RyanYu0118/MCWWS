/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Arrow Pierce Level")
@Description(value={"An arrow's pierce level."})
@Example(value="on shoot:\n\tevent-projectile is an arrow\n\tset arrow pierce level of event-projectile to 5\n")
@RequiredPlugins(value={"Minecraft 1.14+"})
@Since(value={"2.5.1"})
public class ExprArrowPierceLevel
extends SimplePropertyExpression<Projectile, Long> {
    private static final boolean CAN_USE_PIERCE = Skript.methodExists(Arrow.class, "getPierceLevel", new Class[0]);

    @Override
    @Nullable
    public Long convert(Projectile arrow) {
        return ((Arrow)arrow).getPierceLevel();
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET: 
            case RESET: 
            case REMOVE: 
            case ADD: {
                return CollectionUtils.array(Number.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        int strength = delta != null ? Math.max(((Number)delta[0]).intValue(), 0) : 0;
        int mod = 1;
        switch (mode) {
            case REMOVE: {
                mod = -1;
            }
            case ADD: {
                for (Projectile entity : (Projectile[])this.getExpr().getArray(e)) {
                    if (!(entity instanceof Arrow)) continue;
                    Arrow arrow = (Arrow)entity;
                    int dmg = Math.round(arrow.getPierceLevel() + strength * mod);
                    if (dmg < 0) {
                        dmg = 0;
                    }
                    arrow.setPierceLevel(dmg);
                }
                break;
            }
            case SET: 
            case RESET: {
                for (Projectile entity : (Projectile[])this.getExpr().getArray(e)) {
                    ((Arrow)entity).setPierceLevel(strength);
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
        return "arrow pierce level";
    }

    static {
        if (CAN_USE_PIERCE) {
            ExprArrowPierceLevel.register(ExprArrowPierceLevel.class, Long.class, "arrow pierce level", "projectiles");
        }
    }
}

