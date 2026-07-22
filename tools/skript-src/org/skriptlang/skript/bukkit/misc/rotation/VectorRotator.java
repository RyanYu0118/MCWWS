/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Contract
 */
package org.skriptlang.skript.bukkit.misc.rotation;

import java.util.function.Function;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.skriptlang.skript.bukkit.misc.rotation.Rotator;

public class VectorRotator
implements Rotator<Vector> {
    private final Function<Vector, Vector> rotator;

    public VectorRotator(Rotator.Axis axis, double angle) {
        this.rotator = switch (axis) {
            default -> throw new MatchException(null, null);
            case Rotator.Axis.X -> input -> input.rotateAroundX(angle);
            case Rotator.Axis.Y -> input -> input.rotateAroundY(angle);
            case Rotator.Axis.Z -> input -> input.rotateAroundZ(angle);
            case Rotator.Axis.ARBITRARY -> throw new UnsupportedOperationException("Rotation around the " + String.valueOf((Object)axis) + " axis requires additional data. Use a different constructor.");
            case Rotator.Axis.LOCAL_ARBITRARY, Rotator.Axis.LOCAL_X, Rotator.Axis.LOCAL_Y, Rotator.Axis.LOCAL_Z -> input -> input;
        };
    }

    public VectorRotator(Rotator.Axis axis, Vector vector, double angle) {
        this.rotator = switch (axis) {
            default -> throw new MatchException(null, null);
            case Rotator.Axis.X -> input -> input.rotateAroundX(angle);
            case Rotator.Axis.Y -> input -> input.rotateAroundY(angle);
            case Rotator.Axis.Z -> input -> input.rotateAroundZ(angle);
            case Rotator.Axis.ARBITRARY -> input -> input.rotateAroundNonUnitAxis(vector, angle);
            case Rotator.Axis.LOCAL_ARBITRARY, Rotator.Axis.LOCAL_X, Rotator.Axis.LOCAL_Y, Rotator.Axis.LOCAL_Z -> input -> input;
        };
    }

    @Override
    @Contract(value="_ -> param1")
    public Vector rotate(Vector input) {
        return this.rotator.apply(input);
    }
}

