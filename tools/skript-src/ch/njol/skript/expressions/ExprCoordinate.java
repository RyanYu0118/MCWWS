/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Coordinate")
@Description(value={"Represents a given coordinate of a location. "})
@Example(value="player's y-coordinate is smaller than 40:\n\tmessage \"Watch out for lava!\"\n")
@Since(value={"1.4.3"})
public class ExprCoordinate
extends SimplePropertyExpression<Location, Number> {
    private static final char[] axes;
    private int axis;
    static final /* synthetic */ boolean $assertionsDisabled;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        super.init(exprs, matchedPattern, isDelayed, parseResult);
        this.axis = parseResult.mark;
        return true;
    }

    @Override
    public Number convert(Location l) {
        return this.axis == 0 ? l.getX() : (this.axis == 1 ? l.getY() : l.getZ());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if ((mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE) && this.getExpr().isSingle() && Changer.ChangerUtils.acceptsChange(this.getExpr(), Changer.ChangeMode.SET, Location.class)) {
            return new Class[]{Number.class};
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
        if (!$assertionsDisabled && delta == null) {
            throw new AssertionError();
        }
        Location l = (Location)this.getExpr().getSingle(e);
        if (l == null) {
            return;
        }
        double n = ((Number)delta[0]).doubleValue();
        switch (mode) {
            case REMOVE: {
                n = -n;
            }
            case ADD: {
                if (this.axis == 0) {
                    l.setX(l.getX() + n);
                } else if (this.axis == 1) {
                    l.setY(l.getY() + n);
                } else {
                    l.setZ(l.getZ() + n);
                }
                this.getExpr().change(e, new Location[]{l}, Changer.ChangeMode.SET);
                break;
            }
            case SET: {
                if (this.axis == 0) {
                    l.setX(n);
                } else if (this.axis == 1) {
                    l.setY(n);
                } else {
                    l.setZ(n);
                }
                this.getExpr().change(e, new Location[]{l}, Changer.ChangeMode.SET);
                break;
            }
            case DELETE: 
            case REMOVE_ALL: 
            case RESET: {
                if (!$assertionsDisabled) {
                    throw new AssertionError();
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public Expression<? extends Number> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    protected String getPropertyName() {
        return "the " + axes[this.axis] + "-coordinate";
    }

    static {
        boolean bl = $assertionsDisabled = !ExprCoordinate.class.desiredAssertionStatus();
        if (!SkriptConfig.useTypeProperties.value().booleanValue()) {
            ExprCoordinate.register(ExprCoordinate.class, Number.class, "(0\u00a6x|1\u00a6y|2\u00a6z)(-| )(coord[inate]|pos[ition]|loc[ation])[s]", "locations");
        }
        axes = new char[]{'x', 'y', 'z'};
    }
}

