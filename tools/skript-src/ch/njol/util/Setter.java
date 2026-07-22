/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.util;

import java.util.function.Consumer;

@Deprecated(since="2.10.0", forRemoval=true)
@FunctionalInterface
public interface Setter<T>
extends Consumer<T> {
    public void set(T var1);

    @Override
    default public void accept(T t) {
        this.set(t);
    }
}

