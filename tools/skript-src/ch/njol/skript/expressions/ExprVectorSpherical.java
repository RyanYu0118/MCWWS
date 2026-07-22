/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprYawPitch;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Vectors - Spherical Shape")
@Description(value={"Forms a 'spherical shaped' vector using yaw and pitch to manipulate the current point."})
@Example.Examples(value={@Example(value="loop 360 times:\n\tset {_v} to spherical vector radius 1, yaw loop-value, pitch loop-value\n"), @Example(value="set {_v} to spherical vector radius 1, yaw 45, pitch 90")})
@Since(value={"2.2-dev28"})
public class ExprVectorSpherical
extends SimpleExpression<Vector> {
    private static final double DEG_TO_RAD = Math.PI / 180;
    private Expression<Number> radius;
    private Expression<Number> yaw;
    private Expression<Number> pitch;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.radius = exprs[0];
        this.yaw = exprs[1];
        this.pitch = exprs[2];
        return true;
    }

    protected Vector[] get(Event event) {
        Number radius = this.radius.getSingle(event);
        Number yaw = this.yaw.getSingle(event);
        Number pitch = this.pitch.getSingle(event);
        if (radius == null || yaw == null || pitch == null) {
            return null;
        }
        return CollectionUtils.array(ExprVectorSpherical.fromSphericalCoordinates(radius.doubleValue(), ExprYawPitch.fromSkriptYaw(yaw.floatValue()), pitch.floatValue() + 90.0f));
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Vector> getReturnType() {
        return Vector.class;
    }

    @Override
    public Expression<? extends Vector> simplify() {
        if (this.radius instanceof Literal && this.yaw instanceof Literal && this.pitch instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "spherical vector with radius " + this.radius.toString(event, debug) + ", yaw " + this.yaw.toString(event, debug) + " and pitch" + this.pitch.toString(event, debug);
    }

    public static Vector fromSphericalCoordinates(double radius, double theta, double phi) {
        double r = Math.abs(radius);
        double t = theta * (Math.PI / 180);
        double p = phi * (Math.PI / 180);
        double sinp = Math.sin(p);
        double x = r * sinp * Math.cos(t);
        double y = r * Math.cos(p);
        double z = r * sinp * Math.sin(t);
        return new Vector(x, y, z);
    }

    static {
        Skript.registerExpression(ExprVectorSpherical.class, Vector.class, ExpressionType.SIMPLE, "[a] [new] spherical vector [(from|with)] [radius] %number%, [yaw] %number%(,[ and]| and) [pitch] %number%");
    }
}

