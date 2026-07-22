/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util.coll.iterator;

import java.util.List;
import java.util.ListIterator;
import org.jetbrains.annotations.Nullable;

@Deprecated(since="2.10.0", forRemoval=true)
public class ReversedListIterator<T>
implements ListIterator<T> {
    private final ListIterator<T> iter;

    public ReversedListIterator(List<T> list) {
        ListIterator<T> iter = list.listIterator(list.size());
        if (iter == null) {
            throw new IllegalArgumentException(String.valueOf(list));
        }
        this.iter = iter;
    }

    public ReversedListIterator(List<T> list, int index) {
        ListIterator<T> iter = list.listIterator(list.size() - index);
        if (iter == null) {
            throw new IllegalArgumentException(String.valueOf(list));
        }
        this.iter = iter;
    }

    public ReversedListIterator(ListIterator<T> iter) {
        this.iter = iter;
    }

    @Override
    public boolean hasNext() {
        return this.iter.hasPrevious();
    }

    @Override
    @Nullable
    public T next() {
        return this.iter.previous();
    }

    @Override
    public boolean hasPrevious() {
        return this.iter.hasNext();
    }

    @Override
    @Nullable
    public T previous() {
        return this.iter.next();
    }

    @Override
    public int nextIndex() {
        return this.iter.previousIndex();
    }

    @Override
    public int previousIndex() {
        return this.iter.nextIndex();
    }

    @Override
    public void remove() {
        this.iter.remove();
    }

    @Override
    public void set(@Nullable T e) {
        this.iter.set(e);
    }

    @Override
    public void add(@Nullable T e) {
        throw new UnsupportedOperationException();
    }
}

