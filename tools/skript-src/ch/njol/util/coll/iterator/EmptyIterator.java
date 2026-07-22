/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util.coll.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.Nullable;

public final class EmptyIterator<T>
implements Iterator<T> {
    public static final EmptyIterator<Object> instance = new EmptyIterator();

    public static <T> EmptyIterator<T> get() {
        return instance;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public T next() {
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean equals(@Nullable Object obj) {
        return obj instanceof EmptyIterator;
    }

    public int hashCode() {
        return 0;
    }
}

