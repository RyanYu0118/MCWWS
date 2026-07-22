/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util;

import org.jetbrains.annotations.Nullable;

public class SynchronizedReference<V> {
    @Nullable
    private volatile V value;

    public SynchronizedReference(@Nullable V initialValue) {
        this.value = initialValue;
    }

    public SynchronizedReference() {
    }

    @Nullable
    public final V get() {
        assert (Thread.holdsLock(this));
        return this.value;
    }

    public final void set(@Nullable V newValue) {
        assert (Thread.holdsLock(this));
        this.value = newValue;
    }
}

