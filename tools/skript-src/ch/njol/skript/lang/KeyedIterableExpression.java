/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyedValue;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.bukkit.event.Event;

public interface KeyedIterableExpression<T>
extends Expression<T> {
    public boolean canIterateWithKeys();

    public Iterator<KeyedValue<T>> keyedIterator(Event var1);

    default public Stream<KeyedValue<T>> keyedStream(Event event) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this.keyedIterator(event), 0), false);
    }

    @Override
    default public boolean isLoopOf(String input) {
        return this.canIterateWithKeys() && this.isIndexLoop(input);
    }

    default public boolean isIndexLoop(String input) {
        return input.equalsIgnoreCase("index");
    }

    public static boolean canIterateWithKeys(Expression<?> expression) {
        KeyedIterableExpression keyed;
        return expression instanceof KeyedIterableExpression && (keyed = (KeyedIterableExpression)expression).canIterateWithKeys();
    }
}

