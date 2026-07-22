/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util;

import ch.njol.util.Checker;
import java.util.Objects;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

public interface NullableChecker<T>
extends Checker<T>,
Predicate<T> {
    public static final NullableChecker<Object> nullChecker = Objects::nonNull;

    @Override
    public boolean check(@Nullable T var1);

    @Override
    default public boolean test(@Nullable T t) {
        return this.check(t);
    }
}

