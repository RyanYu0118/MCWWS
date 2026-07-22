/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.Expression;

public interface DefaultExpression<T>
extends Expression<T> {
    public boolean init();

    @Override
    public boolean isDefault();
}

