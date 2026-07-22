/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.lang.comparator;

import org.skriptlang.skript.lang.comparator.Comparator;
import org.skriptlang.skript.lang.comparator.ComparatorInfo;
import org.skriptlang.skript.lang.comparator.Relation;

final class InverseComparator<T1, T2>
implements Comparator<T1, T2> {
    private final ComparatorInfo<T2, T1> comparator;

    InverseComparator(ComparatorInfo<T2, T1> comparator) {
        this.comparator = comparator;
    }

    @Override
    public Relation compare(T1 o1, T2 o2) {
        return this.comparator.getComparator().compare(o2, o1).getSwitched();
    }

    @Override
    public boolean supportsOrdering() {
        return this.comparator.getComparator().supportsOrdering();
    }

    @Override
    public boolean supportsInversion() {
        return false;
    }

    public String toString() {
        return "InverseComparator{comparator=" + String.valueOf(this.comparator) + "}";
    }
}

