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

@Name(value="Warning Distance of World Border")
@Description(value={"The warning distance of a world border. The player's screen will be tinted red when they are within this distance of the border.", "Players only see a red tint when approaching a world's worldborder and the warning distance has to be an integer greater than or equal to 0."})
@Example(value="set world border warning distance of {_worldborder} to 1")
@Since(value={"2.11"})
public class ExprWorldBorderWarningDistance
extends SimplePropertyExpression<WorldBorder, Integer> {
    @Override
    @Nullable
    public Integer convert(WorldBorder worldBorder) {
        return worldBorder.getWarningDistance();
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
        int input;
        int n = input = mode == Changer.ChangeMode.RESET ? 5 : ((Number)delta[0]).intValue();
        if (mode != Changer.ChangeMode.RESET && Double.isNaN(((Number)delta[0]).doubleValue())) {
            this.error("NaN is not a valid world border warning distance");
            return;
        }
        block5: for (WorldBorder worldBorder : (WorldBorder[])this.getExpr().getArray(event)) {
            switch (mode) {
                case SET: 
                case RESET: {
                    worldBorder.setWarningDistance(Math.max(input, 0));
                    continue block5;
                }
                case ADD: {
                    if ((long)worldBorder.getWarningDistance() + (long)input > Integer.MAX_VALUE) {
                        worldBorder.setWarningDistance(Integer.MAX_VALUE);
                        continue block5;
                    }
                    if ((long)worldBorder.getWarningDistance() + (long)input < Integer.MIN_VALUE) {
                        worldBorder.setWarningDistance(0);
                        continue block5;
                    }
                    worldBorder.setWarningDistance(Math.max(worldBorder.getWarningDistance() + input, 0));
                    continue block5;
                }
                case REMOVE: {
                    if ((long)worldBorder.getWarningDistance() - (long)input > Integer.MAX_VALUE) {
                        worldBorder.setWarningDistance(Integer.MAX_VALUE);
                        continue block5;
                    }
                    if ((long)worldBorder.getWarningDistance() - (long)input < Integer.MIN_VALUE) {
                        worldBorder.setWarningDistance(0);
                        continue block5;
                    }
                    worldBorder.setWarningDistance(Math.max(worldBorder.getWarningDistance() - input, 0));
                }
            }
        }
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return "world border warning distance";
    }

    static {
        ExprWorldBorderWarningDistance.registerDefault(ExprWorldBorderWarningDistance.class, Integer.class, "world[ ]border warning distance", "worldborders");
    }
}

