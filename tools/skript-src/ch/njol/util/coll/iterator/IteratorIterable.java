/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.util.coll.iterator;

import java.util.Iterator;

public class IteratorIterable<T>
implements Iterable<T> {
    private final Iterator<T> iter;

    public IteratorIterable(Iterator<T> iter) {
        this.iter = iter;
    }

    @Override
    public Iterator<T> iterator() {
        return this.iter;
    }
}

