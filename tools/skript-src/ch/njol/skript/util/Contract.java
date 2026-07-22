/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.lang.Expression;
import org.jetbrains.annotations.Nullable;

public interface Contract {
    public boolean isSingle(Expression<?> ... var1);

    @Nullable
    public Class<?> getReturnType(Expression<?> ... var1);
}

