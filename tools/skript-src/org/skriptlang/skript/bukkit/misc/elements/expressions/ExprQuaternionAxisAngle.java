/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 *  org.joml.AxisAngle4f
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 */
package org.skriptlang.skript.bukkit.misc.elements.expressions;

import ch.njol.skript.Skript;
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
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Rotation Axis/Angle")
@Description(value={"Returns the axis or angle that a quaternion will rotate by/around.", "All quaternions can be represented by a rotation of some amount around some axis, so this expression provides the ability to get that angle/axis."})
@Example.Examples(value={@Example(value="set {_quaternion} to axisAngle(45, vector(1, 2, 3))"), @Example(value="send rotation axis of {_quaternion} # 1, 2, 3"), @Example(value="send rotation angle of {_quaternion} # 45"), @Example(value="set rotation angle of {_quaternion} to 135"), @Example(value="set rotation axis of {_quaternion} to vector(0, 1, 0)")})
@Since(value={"2.10"})
public class ExprQuaternionAxisAngle
extends SimplePropertyExpression<Quaternionf, Object> {
    private boolean isAxis;

    public static void register(SyntaxRegistry syntaxRegistry) {
        if (!Skript.classExists("org.joml.Quaternionf")) {
            return;
        }
        syntaxRegistry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprQuaternionAxisAngle.infoBuilder(ExprQuaternionAxisAngle.class, Object.class, "rotation (angle|:axis)", "quaternions", false).supplier(ExprQuaternionAxisAngle::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.isAxis = parseResult.hasTag("axis");
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Object convert(Quaternionf from) {
        AxisAngle4f axisAngle = new AxisAngle4f();
        axisAngle.set((Quaternionfc)from);
        if (this.isAxis) {
            return new Vector(axisAngle.x, axisAngle.y, axisAngle.z);
        }
        return Float.valueOf((float)((double)(axisAngle.angle * 180.0f) / Math.PI));
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.SET, Changer.ChangeMode.REMOVE -> {
                if (Changer.ChangerUtils.acceptsChange(this.getExpr(), Changer.ChangeMode.SET, Quaternionf.class)) {
                    yield CollectionUtils.array(this.isAxis ? Vector.class : Number.class);
                }
                yield null;
            }
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Object object;
        assert (delta != null);
        Object[] quaternions = (Quaternionf[])this.getExpr().getArray(event);
        AxisAngle4f axisAngle = new AxisAngle4f();
        if (this.isAxis && (object = delta[0]) instanceof Vector) {
            Vector vector = (Vector)object;
            for (Object quaternion : quaternions) {
                axisAngle.set((Quaternionfc)quaternion);
                axisAngle.set(axisAngle.angle, (float)vector.getX(), (float)vector.getY(), (float)vector.getZ());
                quaternion.set(axisAngle);
            }
        } else {
            Object object2 = delta[0];
            if (object2 instanceof Number) {
                Number number = (Number)object2;
                float f = (float)((double)(number.floatValue() / 180.0f) * Math.PI);
                for (Object quaternion : quaternions) {
                    axisAngle.set((Quaternionfc)quaternion);
                    axisAngle.set(f, axisAngle.x, axisAngle.y, axisAngle.z);
                    quaternion.set(axisAngle);
                }
            }
        }
        this.getExpr().change(event, quaternions, Changer.ChangeMode.SET);
    }

    @Override
    public Class<?> getReturnType() {
        return this.isAxis ? Vector.class : Float.class;
    }

    @Override
    public Expression<?> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    protected String getPropertyName() {
        return this.isAxis ? "axis" : "angle";
    }
}

