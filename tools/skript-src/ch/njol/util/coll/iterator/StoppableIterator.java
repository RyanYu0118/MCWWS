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

@Deprecated(since="2.10.2", forRemoval=true)
public class StoppableIterator<T>
implements Iterator<T> {
    private final Iterator<T> iter;
    private final NullableChecker<T> stopper;
    private final boolean returnLast;
    @Nullable
    private T current;
    private boolean stopped = false;
    private boolean calledNext = false;

    public StoppableIterator(Iterator<T> iter, NullableChecker<T> stopper, boolean returnLast) {
        assert (stopper != null);
        this.iter = iter;
        this.stopper = stopper;
        this.returnLast = returnLast;
        if (!returnLast && iter.hasNext()) {
            this.current = iter.next();
        }
    }

    @Override
    public boolean hasNext() {
        boolean cn = this.calledNext;
        this.calledNext = false;
        if (this.stopped || !this.iter.hasNext()) {
            return false;
        }
        if (cn && !this.returnLast) {
            this.current = this.iter.next();
            if (this.stopper.check(this.current)) {
                this.stop();
                return false;
            }
        }
        return true;
    }

    @Override
    @Nullable
    public T next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        this.calledNext = true;
        if (!this.returnLast) {
            return this.current;
        }
        T t = this.iter.next();
        if (this.stopper.check(t)) {
            this.stop();
        }
        return t;
    }

    @Override
    public void remove() {
        this.iter.remove();
    }

    public void stop() {
        this.stopped = true;
    }
}

