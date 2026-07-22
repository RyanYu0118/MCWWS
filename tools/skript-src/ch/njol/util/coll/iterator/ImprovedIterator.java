/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util.coll.iterator;

import java.util.Iterator;
import org.jetbrains.annotations.Nullable;

@Deprecated(since="2.10.0", forRemoval=true)
public class ImprovedIterator<T>
implements Iterator<T> {
    private final Iterator<T> iter;
    @Nullable
    private T current = null;

    public ImprovedIterator(Iterator<T> iter) {
        this.iter = iter;
    }

    @Override
    public boolean hasNext() {
        return this.iter.hasNext();
    }

    @Override
    @Nullable
    public T next() {
        this.current = this.iter.next();
        return this.current;
    }

    @Override
    public void remove() {
        this.iter.remove();
    }

    @Nullable
    public T current() {
        return this.current;
    }
}

