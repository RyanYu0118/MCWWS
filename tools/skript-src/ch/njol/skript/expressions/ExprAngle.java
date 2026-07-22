/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Angle")
@Description(value={"Represents the passed number value in degrees.", "If radians is specified, converts the passed value to degrees. This conversion may not be entirely accurate, due to floating point precision."})
@Example.Examples(value={@Example(value="set {_angle} to 90 degrees"), @Example(value="{_angle} is 90 # true"), @Example(value="180 degrees is pi # true"), @Example(value="pi radians is 180 degrees # true")})
@Since(value={"2.10"})
public class ExprAngle
extends SimpleExpression<Number> {
    private Expression<Number> angle;
    private boolean isRadians;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.angle = expressions[0];
        this.isRadians = matchedPattern % 2 != 0;
        return true;
    }

    protected Number @Nullable [] get(Event event) {
        Number[] numbers = this.angle.getArray(event);
        if (this.isRadians) {
            Number[] degrees = new Double[numbers.length];
            for (int i = 0; i < numbers.length; ++i) {
                degrees[i] = Math.toDegrees(numbers[i].doubleValue());
            }
            return degrees;
        }
        return numbers;
    }

    @Override
    public boolean isSingle() {
        return this.angle.isSingle();
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public Expression<? extends Number> simplify() {
        if (this.angle instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.angle.toString(event, debug) + " in " + (this.isRadians ? "degrees" : "radians");
    }

    static {
        Skript.registerExpression(ExprAngle.class, Number.class, ExpressionType.SIMPLE, "%number% [in] deg[ree][s]", "%number% [in] rad[ian][s]", "%numbers% in deg[ree][s]", "%numbers% in rad[ian][s]");
    }
}

