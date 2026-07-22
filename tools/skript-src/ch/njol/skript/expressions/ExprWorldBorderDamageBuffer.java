/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.WorldBorder
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
import org.bukkit.WorldBorder;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Damage Buffer of World Border")
@Description(value={"The amount of blocks a player may safely be outside the border before taking damage.", "Players only take damage when outside of the world's world border, and the damage buffer distance cannot be less than 0."})
@Example(value="set world border damage buffer of {_worldborder} to 10")
@Since(value={"2.11"})
public class ExprWorldBorderDamageBuffer
extends SimplePropertyExpression<WorldBorder, Double> {
    @Override
    @Nullable
    public Double convert(WorldBorder worldBorder) {
        return worldBorder.getDamageBuffer();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.RESET -> CollectionUtils.array(Number.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        double input;
        double d = input = mode == Changer.ChangeMode.RESET ? 5.0 : ((Number)delta[0]).doubleValue();
        if (Double.isNaN(input)) {
            this.error("NaN is not a valid world border damage buffer");
            return;
        }
        block5: for (WorldBorder worldBorder : (WorldBorder[])this.getExpr().getArray(event)) {
            switch (mode) {
                case SET: 
                case RESET: {
                    worldBorder.setDamageBuffer(Math.max(input, 0.0));
                    continue block5;
                }
                case ADD: {
                    worldBorder.setDamageBuffer(Math.max(worldBorder.getDamageBuffer() + input, 0.0));
                    continue block5;
                }
                case REMOVE: {
                    worldBorder.setDamageBuffer(Math.max(worldBorder.getDamageBuffer() - input, 0.0));
                }
            }
        }
    }

    @Override
    public Class<? extends Double> getReturnType() {
        return Double.class;
    }

    @Override
    protected String getPropertyName() {
        return "world border damage buffer";
    }

    static {
        ExprWorldBorderDamageBuffer.registerDefault(ExprWorldBorderDamageBuffer.class, Double.class, "world[ ]border damage buffer", "worldborders");
    }
}

