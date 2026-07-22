/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package org.skriptlang.skript.bukkit.misc.rotation;

import java.util.function.Function;
import org.jetbrains.annotations.Contract;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.skriptlang.skript.bukkit.misc.rotation.Rotator;

public class NonMutatingQuaternionRotator
implements Rotator<Quaternionf> {
    private final Function<Quaternionf, Quaternionf> rotator;

    public NonMutatingQuaternionRotator(Rotator.Axis axis, float angle) {
        this.rotator = switch (axis) {
            default -> throw new MatchException(null, null);
            case Rotator.Axis.X -> input -> input.rotateLocalX(angle, new Quaternionf());
            case Rotator.Axis.Y -> input -> input.rotateLocalY(angle, new Quaternionf());
            case Rotator.Axis.Z -> input -> input.rotateLocalZ(angle, new Quaternionf());
            case Rotator.Axis.LOCAL_X -> input -> input.rotateX(angle, new Quaternionf());
            case Rotator.Axis.LOCAL_Y -> input -> input.rotateY(angle, new Quaternionf());
            case Rotator.Axis.LOCAL_Z -> input -> input.rotateZ(angle, new Quaternionf());
            case Rotator.Axis.LOCAL_ARBITRARY -> throw new UnsupportedOperationException("Rotation around the " + String.valueOf((Object)axis) + " axis requires additional data. Use a different constructor.");
            case Rotator.Axis.ARBITRARY -> input -> input;
        };
    }

    public NonMutatingQuaternionRotator(Rotator.Axis axis, Vector3f vector, float angle) {
        this.rotator = switch (axis) {
            default -> throw new MatchException(null, null);
            case Rotator.Axis.X -> input -> input.rotateLocalX(angle, new Quaternionf());
            case Rotator.Axis.Y -> input -> input.rotateLocalY(angle, new Quaternionf());
            case Rotator.Axis.Z -> input -> input.rotateLocalZ(angle, new Quaternionf());
            case Rotator.Axis.LOCAL_X -> input -> input.rotateX(angle, new Quaternionf());
            case Rotator.Axis.LOCAL_Y -> input -> input.rotateY(angle, new Quaternionf());
            case Rotator.Axis.LOCAL_Z -> input -> input.rotateZ(angle, new Quaternionf());
            case Rotator.Axis.LOCAL_ARBITRARY -> input -> input.rotateAxis(angle, (Vector3fc)vector, new Quaternionf());
            case Rotator.Axis.ARBITRARY -> input -> input;
        };
    }

    @Override
    @Contract(value="_ -> new")
    public Quaternionf rotate(Quaternionf input) {
        return this.rotator.apply(input);
    }
}

