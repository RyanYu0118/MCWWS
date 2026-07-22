/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.lang.util;

import ch.njol.skript.lang.util.common.AnyAmount;
import ch.njol.skript.util.Container;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Queue;
import org.jetbrains.annotations.NotNull;

@Container.ContainerType(value=Object.class)
public class SkriptQueue
extends LinkedList<Object>
implements Deque<Object>,
Queue<Object>,
AnyAmount,
Container<Object> {
    @Override
    public boolean add(Object element) {
        if (element == null) {
            return false;
        }
        return super.add(element);
    }

    @Override
    public void add(int index, Object element) {
        if (element != null) {
            super.add(index, element);
        }
    }

    @Override
    public void addFirst(Object element) {
        if (element != null) {
            super.addFirst(element);
        }
    }

    @Override
    public void addLast(Object element) {
        if (element != null) {
            super.addLast(element);
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        return super.contains(o);
    }

    @Override
    public Object set(int index, Object element) {
        if (element == null) {
            return null;
        }
        return super.set(index, element);
    }

    @Override
    public boolean addAll(int index, Collection<?> list) {
        ArrayList copy = new ArrayList(list);
        copy.removeIf(Objects::isNull);
        return super.addAll(index, copy);
    }

    @Override
    @NotNull
    public @NotNull Object @NotNull [] toArray() {
        return super.toArray();
    }

    public Object removeSafely(int i) {
        if (i >= 0 && i < this.size()) {
            return this.remove(i);
        }
        return null;
    }

    public Object[] removeRangeSafely(int fromIndex, int toIndex) {
        int from = Math.min(this.size(), Math.min(fromIndex, toIndex));
        int to = Math.max(0, Math.max(fromIndex, toIndex));
        ListIterator it = this.listIterator(from);
        Object[] elements = new Object[to - from];
        int n = to - from;
        for (int i = 0; i < n; ++i) {
            elements[i] = it.next();
            it.remove();
        }
        return elements;
    }

    @Override
    @NotNull
    public Number amount() {
        return this.size();
    }

    @Override
    public Iterator<Object> containerIterator() {
        return new Iterator<Object>(){

            @Override
            public boolean hasNext() {
                return !SkriptQueue.this.isEmpty();
            }

            @Override
            public Object next() {
                return SkriptQueue.this.pollFirst();
            }

            @Override
            public void remove() {
            }
        };
    }
}

