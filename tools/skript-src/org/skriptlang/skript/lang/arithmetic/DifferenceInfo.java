/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.lang.arithmetic;

import org.skriptlang.skript.lang.arithmetic.Operation;

public record DifferenceInfo<T, R>(Class<T> type, Class<R> returnType, Operation<T, T, R> operation) {
    @Deprecated(since="2.13", forRemoval=true)
    public Class<T> getType() {
        return this.type;
    }

    @Deprecated(since="2.13", forRemoval=true)
    public Class<R> getReturnType() {
        return this.returnType;
    }

    @Deprecated(since="2.13", forRemoval=true)
    public Operation<T, T, R> getOperation() {
        return this.operation;
    }
}

