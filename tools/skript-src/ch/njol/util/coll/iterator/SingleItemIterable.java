/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.util.coll.iterator;

import ch.njol.util.coll.iterator.SingleItemIterator;
import java.util.Iterator;

public class SingleItemIterable<T>
implements Iterable<T> {
    private final T item;

    public SingleItemIterable(T item) {
        this.item = item;
    }

    @Override
    public Iterator<T> iterator() {
        return new SingleItemIterator<T>(this.item);
    }
}

