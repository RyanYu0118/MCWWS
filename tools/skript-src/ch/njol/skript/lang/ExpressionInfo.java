/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SyntaxElementInfo;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;

@Deprecated(since="2.14", forRemoval=true)
public class ExpressionInfo<E extends Expression<T>, T>
extends SyntaxElementInfo<E> {
    @Nullable
    public ExpressionType expressionType;
    public Class<T> returnType;

    public ExpressionInfo(String[] patterns, Class<T> returnType, Class<E> expressionClass, String originClassPath) throws IllegalArgumentException {
        this(patterns, returnType, expressionClass, originClassPath, null);
    }

    public ExpressionInfo(String[] patterns, Class<T> returnType, Class<E> expressionClass, String originClassPath, @Nullable ExpressionType expressionType) throws IllegalArgumentException {
        super(patterns, expressionClass, originClassPath);
        this.returnType = returnType;
        this.expressionType = expressionType;
    }

    @ApiStatus.Internal
    protected ExpressionInfo(DefaultSyntaxInfos.Expression<E, T> source) {
        super(source);
        this.returnType = source.returnType();
        this.expressionType = ExpressionType.fromModern(source.priority());
    }

    public Class<T> getReturnType() {
        return this.returnType;
    }

    @Nullable
    public ExpressionType getExpressionType() {
        return this.expressionType;
    }
}

