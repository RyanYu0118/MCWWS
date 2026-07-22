/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.lang.comparator;

import org.skriptlang.skript.lang.comparator.Relation;

@FunctionalInterface
public interface Comparator<T1, T2> {
    public Relation compare(T1 var1, T2 var2);

    default public boolean supportsOrdering() {
        return false;
    }

    default public boolean supportsInversion() {
        return true;
    }
}

