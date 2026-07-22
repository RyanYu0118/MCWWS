/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.Expression;
import org.jetbrains.annotations.Nullable;

public interface Literal<T>
extends Expression<T> {
    public T[] getArray();

    public T getSingle();

    @Override
    @Nullable
    public <R> Literal<? extends R> getConvertedExpression(Class<R> ... var1);

    public T[] getAll();
}

