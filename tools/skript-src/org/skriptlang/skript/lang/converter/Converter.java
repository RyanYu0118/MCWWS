/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.converter;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Converter<F, T> {
    public static final int ALL_CHAINING = 0;
    public static final int NO_LEFT_CHAINING = 1;
    public static final int NO_RIGHT_CHAINING = 2;
    public static final int ALLOW_UNSAFE_CASTS = 4;
    public static final int NO_CHAINING = 3;

    @Nullable
    public T convert(F var1);
}

