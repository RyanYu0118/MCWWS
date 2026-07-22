/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Internal
 */
package ch.njol.util;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Math2 {
    private Math2() {
    }

    public static int fit(int min, int value, int max) {
        assert (min <= max) : min + "," + value + "," + max;
        return Math.min(Math.max(value, min), max);
    }

    public static long fit(long min, long value, long max) {
        assert (min <= max) : min + "," + value + "," + max;
        return Math.min(Math.max(value, min), max);
    }

    public static float fit(float min, float value, float max) {
        assert (min <= max) : min + "," + value + "," + max;
        return Math.min(Math.max(value, min), max);
    }

    public static double fit(double min, double value, double max) {
        assert (min <= max) : min + "," + value + "," + max;
        return Math.min(Math.max(value, min), max);
    }

    public static int mod(int value, int mod) {
        return (value % mod + mod) % mod;
    }

    public static long mod(long value, long mod) {
        return (value % mod + mod) % mod;
    }

    public static float mod(float value, float mod) {
        return (value % mod + mod) % mod;
    }

    public static double mod(double value, double mod) {
        return (value % mod + mod) % mod;
    }

    public static int ceil(float value) {
        return (int)Math.ceil((double)value - 1.0E-10);
    }

    public static int round(float value) {
        return (int)Math.round((double)value + 1.0E-10);
    }

    public static long floor(double value) {
        return (long)Math.floor(value + 1.0E-10);
    }

    public static long ceil(double value) {
        return (long)Math.ceil(value - 1.0E-10);
    }

    public static long round(double value) {
        return Math.round(value + 1.0E-10);
    }

    public static float safe(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    public static long addClamped(long x, long y) {
        boolean causedOverflow;
        long result = x + y;
        boolean bl = causedOverflow = ((x ^ result) & (y ^ result)) < 0L;
        if (causedOverflow) {
            return Long.MAX_VALUE;
        }
        return result;
    }

    public static long multiplyClamped(long x, long y) {
        long ay;
        long result = x * y;
        long ax = Math.abs(x);
        if ((ax | (ay = Math.abs(y))) >>> 31 != 0L && (y != 0L && result / y != x || x == Long.MIN_VALUE && y == -1L)) {
            return x < 0L == y < 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        return result;
    }
}

