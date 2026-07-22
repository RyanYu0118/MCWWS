/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converter;

@Deprecated(since="2.10.0", forRemoval=true)
public abstract class Getter<R, A>
implements Converter<A, R> {
    @Nullable
    public abstract R get(A var1);

    @Override
    @Nullable
    public final R convert(A a) {
        return this.get(a);
    }
}

