/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 */
package org.skriptlang.skript.bukkit.misc.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.skriptlang.skript.bukkit.misc.rotation.NonMutatingQuaternionRotator;
import org.skriptlang.skript.bukkit.misc.rotation.NonMutatingVectorRotator;
import org.skriptlang.skript.bukkit.misc.rotation.Rotator;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Rotated Quaternion/Vector")
@Description(value={"Rotates a quaternion or vector around an axis a set amount of degrees, or around all 3 axes at once.", "Vectors can only be rotated around the global X/Y/Z axes, or an arbitrary vector axis.", "Quaternions are more flexible, allowing rotation around the global or local X/Y/Z axes, arbitrary vectors, or all 3 local axes at once.", "Global axes are the ones in the Minecraft world. Local axes are relative to how the quaternion is already oriented.", "", "Note that rotating a quaternion around a vector results in a rotation around the local vector, so results may not be what you expect. For example, rotating around vector(1, 0, 0) is the same as rotating around the local X axis.", "The same applies to rotations by all three axes at once. In addition, rotating around all three axes of a quaternion/display at once will rotate in ZYX order, meaning the Z rotation will be applied first and the X rotation last."})
@Example.Examples(value={@Example(value="set {_new} to {_quaternion} rotated around x axis by 10 degrees"), @Example(value="set {_new} to {_vector} rotated around vector(1, 1, 1) by 45"), @Example(value="set {_new} to {_quaternion} rotated by x 45, y 90, z 135")})
@Since(value={"2.10"})
public class ExprRotate
extends SimpleExpression<Object> {
    private Expression<?> toRotate;
    private @UnknownNullability Expression<Number> angle;
    private @UnknownNullability Expression<Vector> vector;
    private  @UnknownNullability Rotator.Axis axis;
    private @UnknownNullability Expression<Number> x;
    private @UnknownNullability Expression<Number> y;
    private @UnknownNullability Expression<Number> z;
    private int matchedPattern;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprRotate.class, Object.class).supplier(ExprRotate::new)).addPatterns("%quaternions/vectors% rotated around [the] [global] (:x|:y|:z)(-| )axis by %number%", "%quaternions% rotated around [the|its|their] local (:x|:y|:z)(-| )ax(i|e)s by %number%", "%quaternions/vectors% rotated around [the] %vector% by %number%", "%quaternions% rotated by x %number%, y %number%(, [and]| and) z %number%")).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.toRotate = exprs[0];
        this.matchedPattern = matchedPattern;
        switch (matchedPattern) {
            case 0: 
            case 1: {
                Object axisString = parseResult.tags.get(0).toUpperCase(Locale.ENGLISH);
                if (matchedPattern == 1) {
                    axisString = "LOCAL_" + (String)axisString;
                }
                this.angle = exprs[1];
                this.axis = Rotator.Axis.valueOf((String)axisString);
                break;
            }
            case 2: {
                this.vector = exprs[1];
                this.angle = exprs[2];
                this.axis = Rotator.Axis.ARBITRARY;
                break;
            }
            case 3: {
                this.x = exprs[1];
                this.y = exprs[2];
                this.z = exprs[3];
            }
        }
        return true;
    }

    @Override
    @Nullable
    protected Object[] get(Event event) {
        NonMutatingQuaternionRotator quaternionRotator;
        NonMutatingVectorRotator vectorRotator;
        if (this.matchedPattern == 3) {
            Number x = this.x.getSingle(event);
            Number y = this.y.getSingle(event);
            Number z = this.z.getSingle(event);
            if (x == null || y == null || z == null) {
                return new Quaternionf[0];
            }
            float radX = (float)((double)x.floatValue() * Math.PI / 180.0);
            float radY = (float)((double)y.floatValue() * Math.PI / 180.0);
            float radZ = (float)((double)z.floatValue() * Math.PI / 180.0);
            return this.toRotate.stream(event).map(quaternion -> quaternion.rotateZYX(radZ, radY, radX)).toArray(Quaternionf[]::new);
        }
        Number angle = this.angle.getSingle(event);
        if (angle == null) {
            return new Object[0];
        }
        double radAngle = angle.doubleValue() * Math.PI / 180.0;
        if (Double.isInfinite(radAngle) || Double.isNaN(radAngle)) {
            return new Object[0];
        }
        if (this.axis == Rotator.Axis.ARBITRARY) {
            Vector axis = this.vector.getSingle(event);
            if (axis == null || axis.isZero()) {
                return new Object[0];
            }
            axis.normalize();
            Vector3f jomlAxis = axis.toVector3f();
            vectorRotator = new NonMutatingVectorRotator(Rotator.Axis.ARBITRARY, axis, radAngle);
            quaternionRotator = new NonMutatingQuaternionRotator(Rotator.Axis.LOCAL_ARBITRARY, jomlAxis, (float)radAngle);
        } else {
            vectorRotator = new NonMutatingVectorRotator(this.axis, radAngle);
            quaternionRotator = new NonMutatingQuaternionRotator(this.axis, (float)radAngle);
        }
        return this.toRotate.stream(event).map(object -> {
            if (object instanceof Vector) {
                Vector vectorToRotate = (Vector)object;
                return (Cloneable)vectorRotator.rotate(vectorToRotate);
            }
            if (object instanceof Quaternionf) {
                Quaternionf quaternion = (Quaternionf)object;
                return (Cloneable)quaternionRotator.rotate(quaternion);
            }
            return null;
        }).filter(Objects::nonNull).toArray();
    }

    @Override
    public boolean isSingle() {
        return this.toRotate.isSingle();
    }

    @Override
    public Class<?> getReturnType() {
        return this.matchedPattern == 1 || this.matchedPattern == 3 ? Quaternionf.class : this.toRotate.getReturnType();
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return new Class[]{Quaternionf.class, Vector.class};
    }

    @Override
    public Expression<?> simplify() {
        if (this.toRotate instanceof Literal) {
            switch (this.matchedPattern) {
                case 0: 
                case 1: {
                    if (!(this.angle instanceof Literal)) break;
                    return SimplifiedLiteral.fromExpression(this);
                }
                case 2: {
                    if (!(this.angle instanceof Literal) || !(this.vector instanceof Literal)) break;
                    return SimplifiedLiteral.fromExpression(this);
                }
                case 3: {
                    if (!(this.x instanceof Literal) || !(this.y instanceof Literal) || !(this.z instanceof Literal)) break;
                    return SimplifiedLiteral.fromExpression(this);
                }
            }
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return switch (this.matchedPattern) {
            case 0, 1 -> this.toRotate.toString(event, debug) + " rotated around the " + String.valueOf((Object)this.axis) + "-axis by " + this.angle.toString(event, debug) + " degrees";
            case 2 -> this.toRotate.toString(event, debug) + " rotated around " + this.vector.toString(event, debug) + "-axis by " + this.angle.toString(event, debug) + " degrees";
            case 3 -> this.toRotate.toString(event, debug) + " rotated by x " + this.x.toString(event, debug) + ", y " + this.y.toString(event, debug) + ", and z " + this.z.toString(event, debug);
            default -> "invalid";
        };
    }
}

