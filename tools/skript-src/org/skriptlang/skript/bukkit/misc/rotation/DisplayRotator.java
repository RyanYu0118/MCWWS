/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Display
 *  org.bukkit.util.Transformation
 *  org.jetbrains.annotations.Contract
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 */
package org.skriptlang.skript.bukkit.misc.rotation;

import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Contract;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.skriptlang.skript.bukkit.misc.rotation.QuaternionRotator;
import org.skriptlang.skript.bukkit.misc.rotation.Rotator;

public class DisplayRotator
implements Rotator<Display> {
    private final QuaternionRotator qRotator;

    public DisplayRotator(Rotator.Axis axis, float angle) {
        this.qRotator = new QuaternionRotator(axis, angle);
    }

    public DisplayRotator(Rotator.Axis axis, Vector3f vector, float angle) {
        this.qRotator = new QuaternionRotator(axis, vector, angle);
    }

    @Override
    @Contract(value="_ -> param1")
    public Display rotate(Display input) {
        Transformation transformation = input.getTransformation();
        Quaternionf leftRotation = transformation.getLeftRotation();
        input.setTransformation(new Transformation(transformation.getTranslation(), this.qRotator.rotate(leftRotation), transformation.getScale(), transformation.getRightRotation()));
        return input;
    }
}

