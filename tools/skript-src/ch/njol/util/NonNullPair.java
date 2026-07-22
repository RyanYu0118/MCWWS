/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.util;

import ch.njol.util.Pair;

@Deprecated(since="2.10.0", forRemoval=true)
public class NonNullPair<T1, T2>
extends Pair<T1, T2> {
    private static final long serialVersionUID = 820250942098905541L;

    public NonNullPair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    public NonNullPair(NonNullPair<T1, T2> other) {
        this.first = other.first;
        this.second = other.second;
    }

    @Override
    public T1 getFirst() {
        return (T1)this.first;
    }

    @Override
    public void setFirst(T1 first) {
        this.first = first;
    }

    @Override
    public T2 getSecond() {
        return (T2)this.second;
    }

    @Override
    public void setSecond(T2 second) {
        this.second = second;
    }

    @Override
    public T1 getKey() {
        return (T1)this.first;
    }

    @Override
    public T2 getValue() {
        return (T2)this.second;
    }

    @Override
    public T2 setValue(T2 value) {
        Object old = this.second;
        this.second = value;
        return (T2)old;
    }

    @Override
    public NonNullPair<T1, T2> clone() {
        return new NonNullPair<T1, T2>(this);
    }
}

