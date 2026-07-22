/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util;

import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

@Deprecated(since="2.10.0", forRemoval=true)
@FunctionalInterface
public interface Callback<R, A>
extends Function<A, R> {
    @Nullable
    public R run(A var1);

    @Override
    default public R apply(A a) {
        return this.run(a);
    }
}

