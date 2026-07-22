/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.PeekingIterator
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util.coll.iterator;

import com.google.common.collect.PeekingIterator;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.Nullable;

public class ArrayIterator<T>
implements PeekingIterator<T> {
    @Nullable
    private final T[] array;
    private int index = 0;

    public ArrayIterator(@Nullable T[] array) {
        this.array = array;
    }

    public ArrayIterator(@Nullable T[] array, int start) {
        this.array = array;
        this.index = start;
    }

    public boolean hasNext() {
        T[] array = this.array;
        if (array == null) {
            return false;
        }
        return this.index < array.length;
    }

    public T peek() {
        int peekIndex = this.index + 1;
        if (this.array == null || peekIndex >= this.array.length) {
            return null;
        }
        return this.array[peekIndex];
    }

    @Nullable
    public T next() {
        T[] array = this.array;
        if (array == null || this.index >= array.length) {
            throw new NoSuchElementException();
        }
        return array[this.index++];
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}

