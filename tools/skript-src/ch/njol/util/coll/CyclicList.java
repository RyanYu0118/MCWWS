/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util.coll;

import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;

@Deprecated(since="2.10.0", forRemoval=true)
public final class CyclicList<E>
extends AbstractList<E> {
    private final Object[] items;
    private int start = 0;

    public CyclicList(int size) {
        this.items = new Object[size];
    }

    public CyclicList(E[] array) {
        this.items = new Object[array.length];
        System.arraycopy(array, 0, this.items, 0, array.length);
    }

    public CyclicList(Collection<E> c) {
        Object[] items = c.toArray();
        if (items == null) {
            throw new IllegalArgumentException(String.valueOf(c));
        }
        this.items = items;
    }

    private final int toInternalIndex(int index) {
        return Math2.mod(this.start + index, this.items.length);
    }

    private final int toExternalIndex(int internal) {
        return Math2.mod(internal - this.start, this.items.length);
    }

    @Override
    public boolean add(@Nullable E e) {
        this.addLast(e);
        return true;
    }

    @Override
    public void addFirst(@Nullable E e) {
        this.start = Math2.mod(this.start - 1, this.items.length);
        this.items[this.start] = e;
    }

    @Override
    public void addLast(@Nullable E e) {
        this.items[this.start] = e;
        this.start = Math2.mod(this.start + 1, this.items.length);
    }

    @Override
    public void add(int index, E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (E e : c) {
            this.add(e);
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    private void rangeCheck(int index) {
        if (index < 0 || index >= this.items.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.items.length);
        }
    }

    @Override
    @Nullable
    public E get(int index) {
        this.rangeCheck(index);
        return (E)this.items[this.toInternalIndex(index)];
    }

    @Override
    public int indexOf(@Nullable Object o) {
        return this.toExternalIndex(CollectionUtils.indexOf(this.items, o));
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int lastIndexOf(@Nullable Object o) {
        return this.toExternalIndex(CollectionUtils.lastIndexOf(this.items, o));
    }

    @Override
    public boolean remove(@Nullable Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@Nullable Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@Nullable Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nullable
    public E set(int index, @Nullable E e) {
        this.rangeCheck(index);
        int i = this.toInternalIndex(index);
        Object old = this.items[i];
        this.items[i] = e;
        return (E)old;
    }

    @Override
    public int size() {
        return this.items.length;
    }

    @Override
    public Object[] toArray() {
        return this.toArray(new Object[this.items.length]);
    }

    @Override
    public <T> T[] toArray(@Nullable T[] array) {
        if (array == null) {
            return this.toArray();
        }
        if (array.length < this.items.length) {
            return this.toArray((Object[])Array.newInstance(array.getClass().getComponentType(), this.items.length));
        }
        System.arraycopy(this.items, this.start, array, 0, this.items.length - this.start);
        System.arraycopy(this.items, 0, array, this.items.length - this.start, this.start);
        if (array.length > this.items.length) {
            array[this.items.length] = null;
        }
        return array;
    }
}

