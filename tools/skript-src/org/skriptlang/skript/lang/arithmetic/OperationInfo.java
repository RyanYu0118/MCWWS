/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.arithmetic;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Operation;
import org.skriptlang.skript.lang.converter.Converters;

public record OperationInfo<L, R, T>(Class<L> left, Class<R> right, Class<T> returnType, Operation<L, R, T> operation) {
    public OperationInfo(@NotNull Class<L> left, @NotNull Class<R> right, @NotNull Class<T> returnType, @NotNull Operation<L, R, T> operation) {
        Preconditions.checkNotNull(left, (Object)"Cannot do arithmetic with nothing and something! (left is null)");
        Preconditions.checkNotNull(right, (Object)"Cannot do arithmetic with something and nothing! (right is null)");
        Preconditions.checkNotNull(returnType, (Object)"Cannot have nothing as the result of arithmetic! (returnType is null)");
        Preconditions.checkNotNull(operation, (Object)"Cannot do arithmetic with a null operation!");
    }

    @Deprecated(since="2.13", forRemoval=true)
    public Class<L> getLeft() {
        return this.left;
    }

    @Deprecated(since="2.13", forRemoval=true)
    public Class<R> getRight() {
        return this.right;
    }

    @Deprecated(since="2.13", forRemoval=true)
    public Class<T> getReturnType() {
        return this.returnType;
    }

    @Deprecated(since="2.13", forRemoval=true)
    public Operation<L, R, T> getOperation() {
        return this.operation;
    }

    @Nullable
    public <L2, R2> OperationInfo<L2, R2, T> getConverted(Class<L2> fromLeft, Class<R2> fromRight) {
        return this.getConverted(fromLeft, fromRight, this.returnType);
    }

    @Nullable
    public <L2, R2, T2> OperationInfo<L2, R2, T2> getConverted(Class<L2> fromLeft, Class<R2> fromRight, Class<T2> toReturnType) {
        if (fromLeft == Object.class || fromRight == Object.class) {
            return null;
        }
        if (!(Converters.converterExists(fromLeft, this.left) && Converters.converterExists(fromRight, this.right) && Converters.converterExists(this.returnType, toReturnType))) {
            return null;
        }
        return new OperationInfo<Object, Object, Object>(fromLeft, fromRight, toReturnType, (left, right) -> {
            L convertedLeft = Converters.convert(left, this.left);
            R convertedRight = Converters.convert(right, this.right);
            if (convertedLeft == null || convertedRight == null) {
                return null;
            }
            T result = this.operation.calculate(convertedLeft, convertedRight);
            return Converters.convert(result, toReturnType);
        });
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper((Object)this).add("left", this.left).add("right", this.right).add("returnType", this.returnType).toString();
    }
}

