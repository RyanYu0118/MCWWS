/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util.coll.iterator;

import ch.njol.util.NullableChecker;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.Nullable;

public class CheckedIterator<T>
implements Iterator<T> {
    private final Iterator<T> iter;
    private final NullableChecker<T> checker;
    private boolean returnedNext = true;
    @Nullable
    private T next;

    public CheckedIterator(Iterator<T> iter, NullableChecker<T> checker) {
        this.iter = iter;
        this.checker = checker;
    }

    @Override
    public boolean hasNext() {
        if (!this.returnedNext) {
            return true;
        }
        if (!this.iter.hasNext()) {
            return false;
        }
        while (this.iter.hasNext()) {
            this.next = this.iter.next();
            if (!this.checker.check(this.next)) continue;
            this.returnedNext = false;
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public T next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        this.returnedNext = true;
        return this.next;
    }

    @Override
    public void remove() {
        this.iter.remove();
    }
}

