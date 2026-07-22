/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Axis
 */
package org.skriptlang.skript.bukkit.misc.rotation;

import java.util.Locale;

@FunctionalInterface
public interface Rotator<T> {
    public T rotate(T var1);

    public static enum Axis {
        X,
        LOCAL_X,
        Y,
        LOCAL_Y,
        Z,
        LOCAL_Z,
        ARBITRARY,
        LOCAL_ARBITRARY;


        public String toString() {
            return super.toString().toLowerCase(Locale.ENGLISH).replace("_", " ");
        }

        public static Axis fromBukkit(org.bukkit.Axis axis) {
            return switch (axis) {
                default -> throw new MatchException(null, null);
                case org.bukkit.Axis.X -> X;
                case org.bukkit.Axis.Y -> Y;
                case org.bukkit.Axis.Z -> Z;
            };
        }
    }
}

