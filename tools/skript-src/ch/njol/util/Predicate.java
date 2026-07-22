/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package ch.njol.util;

import javax.annotation.Nullable;

@Deprecated(since="2.10.0", forRemoval=true)
@FunctionalInterface
public interface Predicate<T>
extends java.util.function.Predicate<T> {
    @Override
    public boolean test(@Nullable T var1);
}

