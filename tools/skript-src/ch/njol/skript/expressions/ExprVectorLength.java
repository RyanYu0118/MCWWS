/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
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
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.util.coll.CollectionUtils;
import java.util.function.Function;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Vectors - Length")
@Description(value={"Gets or sets the length of a vector."})
@Example.Examples(value={@Example(value="send \"%standard length of vector 1, 2, 3%\""), @Example(value="set {_v} to vector 1, 2, 3"), @Example(value="set standard length of {_v} to 2"), @Example(value="send \"%standard length of {_v}%\"")})
@Since(value={"2.2-dev28"})
public class ExprVectorLength
extends SimplePropertyExpression<Vector, Number> {
    @Override
    public Number convert(Vector vector) {
        return vector.length();
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.ADD || mode == Changer.ChangeMode.REMOVE || mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(Number.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Function<Vector, Vector> changeFunction;
        assert (delta != null);
        double deltaLength = ((Number)delta[0]).doubleValue();
        switch (mode) {
            case REMOVE: {
                deltaLength = -deltaLength;
            }
            case ADD: {
                double finalDeltaLength = deltaLength;
                double finalDeltaLengthSquared = deltaLength * deltaLength;
                changeFunction = vector -> {
                    if (vector.isZero() || finalDeltaLength < 0.0 && vector.lengthSquared() < finalDeltaLengthSquared) {
                        vector.zero();
                    } else {
                        double newLength = finalDeltaLength + vector.length();
                        if (!vector.isNormalized()) {
                            vector.normalize();
                        }
                        vector.multiply(newLength);
                    }
                    return vector;
                };
                break;
            }
            case SET: {
                double finalDeltaLength1 = deltaLength;
                changeFunction = vector -> {
                    if (finalDeltaLength1 < 0.0 || vector.isZero()) {
                        vector.zero();
                    } else {
                        if (!vector.isNormalized()) {
                            vector.normalize();
                        }
                        vector.multiply(finalDeltaLength1);
                    }
                    return vector;
                };
                break;
            }
            default: {
                return;
            }
        }
        this.getExpr().changeInPlace(event, changeFunction);
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
        return "vector length";
    }

    static {
        ExprVectorLength.register(ExprVectorLength.class, Number.class, "(vector|standard|normal) length[s]", "vectors");
    }
}

