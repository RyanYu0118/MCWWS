/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions.arithmetic;

import ch.njol.skript.expressions.arithmetic.ArithmeticGettable;
import ch.njol.skript.lang.Expression;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;

public record ArithmeticExpressionInfo<T>(Expression<? extends T> expression) implements ArithmeticGettable<T>
{
    @Override
    @Nullable
    public T get(Event event) {
        T object = this.expression.getSingle(event);
        return (T)(object == null ? Arithmetics.getDefaultValue(this.expression.getReturnType()) : object);
    }

    @Override
    public Class<? extends T> getReturnType() {
        return this.expression.getReturnType();
    }
}

