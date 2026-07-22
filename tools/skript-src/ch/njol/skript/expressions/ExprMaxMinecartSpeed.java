/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Minecart
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Max Minecart Speed")
@Description(value={"The maximum speed of a minecart."})
@Example(value="on right click on minecart:\n\tset max minecart speed of event-entity to 1\n")
@Since(value={"2.5.1"})
public class ExprMaxMinecartSpeed
extends SimplePropertyExpression<Entity, Number> {
    @Override
    @Nullable
    public Number convert(Entity entity) {
        return entity instanceof Minecart ? Double.valueOf(((Minecart)entity).getMaxSpeed()) : null;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case ADD: 
            case REMOVE: 
            case RESET: 
            case SET: {
                return CollectionUtils.array(Number.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (delta == null) {
            if (mode == Changer.ChangeMode.RESET) {
                for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                    if (!(entity instanceof Minecart)) continue;
                    ((Minecart)entity).setMaxSpeed(0.4);
                }
            }
            return;
        }
        int mod = 1;
        switch (mode) {
            case SET: {
                for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                    if (!(entity instanceof Minecart)) continue;
                    ((Minecart)entity).setMaxSpeed(((Number)delta[0]).doubleValue());
                }
                break;
            }
            case REMOVE: {
                mod = -1;
            }
            case ADD: {
                for (Entity entity : (Entity[])this.getExpr().getArray(e)) {
                    if (!(entity instanceof Minecart)) continue;
                    Minecart minecart = (Minecart)entity;
                    minecart.setMaxSpeed(minecart.getMaxSpeed() + ((Number)delta[0]).doubleValue() * (double)mod);
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
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "max minecart speed";
    }

    static {
        ExprMaxMinecartSpeed.register(ExprMaxMinecartSpeed.class, Number.class, "max[imum] minecart (speed|velocity)", "entities");
    }
}

