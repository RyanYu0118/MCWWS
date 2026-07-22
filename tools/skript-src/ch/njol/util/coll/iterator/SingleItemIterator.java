/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.util.coll.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SingleItemIterator<T>
implements Iterator<T> {
    private final T item;
    private boolean returned = false;

    public SingleItemIterator(T item) {
        this.item = item;
    }

    @Override
    public boolean hasNext() {
        return !this.returned;
    }

    @Override
    public T next() {
        if (this.returned) {
            throw new NoSuchElementException();
        }
        this.returned = true;
        return this.item;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

