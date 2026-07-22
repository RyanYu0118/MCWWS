/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.util.coll.iterator;

import java.util.Iterator;
import java.util.function.Consumer;

public class ConsumingIterator<E>
implements Iterator<E> {
    private final Iterator<E> iterator;
    private final Consumer<E> consumer;

    public ConsumingIterator(Iterator<E> iterator, Consumer<E> consumer) {
        this.iterator = iterator;
        this.consumer = consumer;
    }

    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    @Override
    public E next() {
        E value = this.iterator.next();
        this.consumer.accept(value);
        return value;
    }

    @Override
    public void remove() {
        this.iterator.remove();
    }
}

