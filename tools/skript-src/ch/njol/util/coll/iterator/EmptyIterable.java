/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util.coll.iterator;

import ch.njol.util.coll.iterator.EmptyIterator;
import java.util.Iterator;
import org.jetbrains.annotations.Nullable;

public final class EmptyIterable<T>
implements Iterable<T> {
    public static final EmptyIterable<Object> instance = new EmptyIterable();

    public static <T> EmptyIterable<T> get() {
        return instance;
    }

    @Override
    public Iterator<T> iterator() {
        return EmptyIterator.get();
    }

    public boolean equals(@Nullable Object obj) {
        return obj instanceof EmptyIterable;
    }

    public int hashCode() {
        return 0;
    }
}

