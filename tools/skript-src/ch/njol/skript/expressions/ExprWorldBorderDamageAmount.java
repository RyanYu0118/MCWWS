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

@Name(value="Damage Amount of World Border")
@Description(value={"The amount of damage a player takes per second for each block they are outside the border plus the border buffer.", "Players only take damage when outside of the world's world border, and the damage value cannot be less than 0."})
@Example(value="set world border damage amount of {_worldborder} to 1")
@Since(value={"2.11"})
public class ExprWorldBorderDamageAmount
extends SimplePropertyExpression<WorldBorder, Double> {
    @Override
    @Nullable
    public Double convert(WorldBorder worldBorder) {
        return worldBorder.getDamageAmount();
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
        double d = input = mode == Changer.ChangeMode.RESET ? 0.2 : ((Number)delta[0]).doubleValue();
        if (Double.isNaN(input)) {
            this.error("NaN is not a valid world border damage amount");
            return;
        }
        if (Double.isInfinite(input)) {
            this.error("World border damage amount cannot be infinite");
            return;
        }
        block5: for (WorldBorder worldBorder : (WorldBorder[])this.getExpr().getArray(event)) {
            switch (mode) {
                case SET: 
                case RESET: {
                    worldBorder.setDamageAmount(Math.max(input, 0.0));
                    continue block5;
                }
                case ADD: {
                    worldBorder.setDamageAmount(Math.max(worldBorder.getDamageAmount() + input, 0.0));
                    continue block5;
                }
                case REMOVE: {
                    worldBorder.setDamageAmount(Math.max(worldBorder.getDamageAmount() - input, 0.0));
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
        return "world border damage amount";
    }

    static {
        ExprWorldBorderDamageAmount.registerDefault(ExprWorldBorderDamageAmount.class, Double.class, "world[ ]border damage amount", "worldborders");
    }
}

