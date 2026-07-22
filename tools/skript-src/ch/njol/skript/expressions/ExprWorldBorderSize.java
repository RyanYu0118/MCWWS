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
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.WorldBorder;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Size of World Border")
@Description(value={"The size of a world border.", "The size can not be smaller than 1."})
@Example(value="set world border radius of {_worldborder} to 10")
@Since(value={"2.11"})
public class ExprWorldBorderSize
extends SimplePropertyExpression<WorldBorder, Double> {
    private boolean radius;
    private static final double MAX_WORLDBORDER_SIZE = 5.9999968E7;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.radius = parseResult.hasTag("radius");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Double convert(WorldBorder worldBorder) {
        return worldBorder.getSize() * (this.radius ? 0.5 : 1.0);
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
        double d = mode == Changer.ChangeMode.RESET ? 5.9999968E7 : (input = ((Number)delta[0]).doubleValue() * (double)(this.radius ? 2 : 1));
        if (Double.isNaN(input)) {
            this.error("NaN is not a valid world border size");
            return;
        }
        block5: for (WorldBorder worldBorder : (WorldBorder[])this.getExpr().getArray(event)) {
            switch (mode) {
                case SET: 
                case RESET: {
                    worldBorder.setSize(Math2.fit(1.0, input, 5.9999968E7));
                    continue block5;
                }
                case ADD: {
                    worldBorder.setSize(Math2.fit(1.0, worldBorder.getSize() + input, 5.9999968E7));
                    continue block5;
                }
                case REMOVE: {
                    worldBorder.setSize(Math2.fit(1.0, worldBorder.getSize() - input, 5.9999968E7));
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
        return "world border size";
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "world border " + (this.radius ? "radius" : "diameter") + " of " + this.getExpr().toString(event, debug);
    }

    static {
        ExprWorldBorderSize.registerDefault(ExprWorldBorderSize.class, Double.class, "world[ ]border (size|diameter|:radius)", "worldborders");
    }
}

