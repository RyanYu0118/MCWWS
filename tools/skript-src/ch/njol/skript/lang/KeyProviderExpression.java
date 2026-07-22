/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyedIterableExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.util.coll.iterator.ArrayIterator;
import java.util.Iterator;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public interface KeyProviderExpression<T>
extends Expression<T>,
KeyedIterableExpression<T> {
    @NotNull
    public @NotNull String @NotNull [] getArrayKeys(Event var1) throws IllegalStateException;

    @NotNull
    default public @NotNull String @NotNull [] getAllKeys(Event event) {
        return this.getArrayKeys(event);
    }

    @Override
    default public Iterator<KeyedValue<T>> keyedIterator(Event event) {
        return new ArrayIterator(KeyedValue.zip(this.getArray(event), this.getArrayKeys(event)));
    }

    @Override
    default public boolean isSingle() {
        return false;
    }

    default public boolean canReturnKeys() {
        return true;
    }

    @Override
    default public boolean canIterateWithKeys() {
        return this.canReturnKeys();
    }

    default public boolean areKeysRecommended() {
        return true;
    }

    public static boolean canReturnKeys(Expression<?> expression) {
        KeyProviderExpression provider;
        return expression instanceof KeyProviderExpression && (provider = (KeyProviderExpression)expression).canReturnKeys();
    }

    public static boolean areKeysRecommended(Expression<?> expression) {
        return KeyProviderExpression.canReturnKeys(expression) && ((KeyProviderExpression)expression).areKeysRecommended();
    }
}

