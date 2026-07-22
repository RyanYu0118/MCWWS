/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util.coll;

import ch.njol.util.coll.iterator.ReversedListIterator;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.jetbrains.annotations.Nullable;

@Deprecated(since="2.10.0", forRemoval=true)
public class ReversedListView<T>
implements List<T> {
    private final List<T> list;

    public ReversedListView(List<T> list) {
        assert (list != null);
        this.list = list;
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return this.list.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new ReversedListIterator<T>(this.list);
    }

    @Override
    public ListIterator<T> listIterator() {
        return new ReversedListIterator<T>(this.list);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new ReversedListIterator<T>(this.list, index);
    }

    @Override
    public Object[] toArray() {
        Object[] r = new Object[this.size()];
        int i = 0;
        for (T o : this) {
            r[i++] = o;
        }
        return r;
    }

    @Override
    public <R> R[] toArray(R[] a) {
        R[] t = a.length >= this.size() ? a : (Object[])Array.newInstance(a.getClass().getComponentType(), this.size());
        int i = 0;
        for (T o : this) {
            t[i++] = o;
        }
        if (t.length > this.size()) {
            t[this.size()] = null;
        }
        return t;
    }

    @Override
    public boolean add(T e) {
        this.list.add(0, e);
        return true;
    }

    @Override
    public boolean remove(@Nullable Object o) {
        int i = this.list.lastIndexOf(o);
        if (i != -1) {
            this.list.remove(i);
        }
        return i != -1;
    }

    @Override
    public boolean containsAll(@Nullable Collection<?> c) {
        return this.list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T o : c) {
            this.list.add(0, o);
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        int i = this.size() - index;
        for (T o : c) {
            this.list.add(i, o);
        }
        return true;
    }

    @Override
    public boolean removeAll(@Nullable Collection<?> c) {
        return this.list.removeAll(c);
    }

    @Override
    public boolean retainAll(@Nullable Collection<?> c) {
        return this.list.retainAll(c);
    }

    @Override
    public void clear() {
        this.list.clear();
    }

    @Override
    @Nullable
    public T get(int index) {
        return this.list.get(this.size() - index - 1);
    }

    @Override
    @Nullable
    public T set(int index, T element) {
        return this.list.set(this.size() - index - 1, element);
    }

    @Override
    public void add(int index, T element) {
        this.list.add(this.size() - index, element);
    }

    @Override
    @Nullable
    public T remove(int index) {
        return this.list.remove(this.size() - index - 1);
    }

    @Override
    public int indexOf(@Nullable Object o) {
        return this.size() - this.list.lastIndexOf(o) - 1;
    }

    @Override
    public int lastIndexOf(@Nullable Object o) {
        return this.size() - this.list.indexOf(o) - 1;
    }

    @Override
    public ReversedListView<T> subList(int fromIndex, int toIndex) {
        List<T> l = this.list.subList(this.size() - toIndex, this.size() - fromIndex);
        if (l == null) {
            throw new UnsupportedOperationException(String.valueOf(this.list));
        }
        return new ReversedListView<T>(l);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (T e : this) {
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        }
        return hashCode;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof List)) {
            return false;
        }
        List other = (List)obj;
        if (other.size() != this.size()) {
            return false;
        }
        Iterator os = other.iterator();
        for (T t : this) {
            Object o = os.next();
            if (!(t == null ? o != null : !t.equals(o))) continue;
            return false;
        }
        return true;
    }
}

