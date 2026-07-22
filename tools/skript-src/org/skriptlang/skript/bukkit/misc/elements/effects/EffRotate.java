/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Display
 *  org.bukkit.event.Event
 *  org.bukkit.util.Transformation
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 */
package org.skriptlang.skript.bukkit.misc.elements.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.Locale;
import org.bukkit.entity.Display;
import org.bukkit.event.Event;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.skriptlang.skript.bukkit.misc.rotation.DisplayRotator;
import org.skriptlang.skript.bukkit.misc.rotation.QuaternionRotator;
import org.skriptlang.skript.bukkit.misc.rotation.Rotator;
import org.skriptlang.skript.bukkit.misc.rotation.VectorRotator;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Rotate")
@Description(value={"Rotates displays, quaternions, or vectors around an axis a set amount of degrees, or around all 3 axes at once.", "Vectors can only be rotated around the global X/Y/Z axes, or an arbitrary vector axis.", "Quaternions are more flexible, allowing rotation around the global or local X/Y/Z axes, arbitrary vectors, or all 3 local axes at once.", "Global axes are the ones in the Minecraft world. Local axes are relative to how the quaternion is already oriented.", "", "Rotating a display is a shortcut for rotating its left rotation. If the right rotation needs to be modified, it should be acquired, rotated, and re-set.", "", "Note that rotating a quaternion/display around a vector results in a rotation around the local vector, so results may not be what you expect. For example, rotating quaternions/displays around vector(1, 0, 0) is the same as rotating around the local X axis.", "The same applies to rotations by all three axes at once. In addition, rotating around all three axes of a quaternion/display at once will rotate in ZYX order, meaning the Z rotation will be applied first and the X rotation last."})
@Example.Examples(value={@Example(value="rotate {_quaternion} around x axis by 10 degrees"), @Example(value="rotate last spawned block display around y axis by 10 degrees"), @Example(value="rotate {_vector} around vector(1, 1, 1) by 45"), @Example(value="rotate {_quaternion} by x 45, y 90, z 135")})
@Since(value={"2.2-dev28, 2.10 (quaternions, displays)"})
public class EffRotate
extends Effect {
    private Expression<?> toRotate;
    private @UnknownNullability Expression<Number> angle;
    private @UnknownNullability Expression<Vector> vector;
    private  @UnknownNullability Rotator.Axis axis;
    private @UnknownNullability Expression<Number> x;
    private @UnknownNullability Expression<Number> y;
    private @UnknownNullability Expression<Number> z;
    private int matchedPattern;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffRotate.class).supplier(EffRotate::new).addPatterns("rotate %vectors/quaternions/displays% around [the] [global] (:x|:y|:z)(-| )axis by %number%", "rotate %quaternions/displays% around [the|its|their] local (:x|:y|:z)(-| )ax(i|e)s by %number%", "rotate %vectors/quaternions/displays% around [the] %vector% by %number%", "rotate %quaternions/displays% by x %number%, y %number%(, [and]| and) z %number%").build());
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
    protected void execute(Event event) {
        DisplayRotator displayRotator;
        QuaternionRotator quaternionRotator;
        VectorRotator vectorRotator;
        if (this.matchedPattern == 3) {
            Number x = this.x.getSingle(event);
            Number y = this.y.getSingle(event);
            Number z = this.z.getSingle(event);
            if (x == null || y == null || z == null) {
                return;
            }
            float radX = (float)((double)x.floatValue() * Math.PI / 180.0);
            float radY = (float)((double)y.floatValue() * Math.PI / 180.0);
            float radZ = (float)((double)z.floatValue() * Math.PI / 180.0);
            for (Object object : this.toRotate.getArray(event)) {
                if (object instanceof Quaternionf) {
                    Quaternionf quaternion = (Quaternionf)object;
                    quaternion.rotateZYX(radZ, radY, radX);
                    continue;
                }
                if (!(object instanceof Display)) continue;
                Display display = (Display)object;
                Transformation transformation = display.getTransformation();
                Quaternionf leftRotation = transformation.getLeftRotation();
                display.setTransformation(new Transformation(transformation.getTranslation(), leftRotation.rotateZYX(radZ, radY, radX), transformation.getScale(), transformation.getRightRotation()));
            }
            return;
        }
        Number angle = this.angle.getSingle(event);
        if (angle == null) {
            return;
        }
        double radAngle = angle.doubleValue() * Math.PI / 180.0;
        if (Double.isInfinite(radAngle) || Double.isNaN(radAngle)) {
            return;
        }
        if (this.axis == Rotator.Axis.ARBITRARY) {
            Vector axis = this.vector.getSingle(event);
            if (axis == null || axis.isZero()) {
                return;
            }
            axis.normalize();
            Vector3f jomlAxis = axis.toVector3f();
            vectorRotator = new VectorRotator(Rotator.Axis.ARBITRARY, axis, radAngle);
            quaternionRotator = new QuaternionRotator(Rotator.Axis.LOCAL_ARBITRARY, jomlAxis, (float)radAngle);
            displayRotator = new DisplayRotator(Rotator.Axis.LOCAL_ARBITRARY, jomlAxis, (float)radAngle);
        } else {
            vectorRotator = new VectorRotator(this.axis, radAngle);
            quaternionRotator = new QuaternionRotator(this.axis, (float)radAngle);
            displayRotator = new DisplayRotator(this.axis, (float)radAngle);
        }
        for (Object object : this.toRotate.getArray(event)) {
            if (object instanceof Vector) {
                Vector vectorToRotate = (Vector)object;
                vectorRotator.rotate(vectorToRotate);
                continue;
            }
            if (object instanceof Quaternionf) {
                Quaternionf quaternion = (Quaternionf)object;
                quaternionRotator.rotate(quaternion);
                continue;
            }
            if (!(object instanceof Display)) continue;
            Display display = (Display)object;
            displayRotator.rotate(display);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return switch (this.matchedPattern) {
            case 0, 1 -> "rotate " + this.toRotate.toString(event, debug) + " around the " + String.valueOf((Object)this.axis) + "-axis by " + this.angle.toString(event, debug) + " degrees";
            case 2 -> "rotate " + this.toRotate.toString(event, debug) + " around " + this.vector.toString(event, debug) + "-axis by " + this.angle.toString(event, debug) + " degrees";
            case 3 -> "rotate " + this.toRotate.toString(event, debug) + " by x " + this.x.toString(event, debug) + ", y " + this.y.toString(event, debug) + ", and z " + this.z.toString(event, debug);
            default -> "invalid";
        };
    }
}

