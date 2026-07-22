/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.util;

import java.util.function.Predicate;

@Deprecated(since="2.10.0", forRemoval=true)
@FunctionalInterface
public interface Checker<T>
extends Predicate<T> {
    public boolean check(T var1);

    @Override
    default public boolean test(T t) {
        return this.check(t);
    }
}

