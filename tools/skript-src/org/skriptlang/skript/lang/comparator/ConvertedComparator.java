/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.comparator;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.comparator.Comparator;
import org.skriptlang.skript.lang.comparator.ComparatorInfo;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.lang.converter.ConverterInfo;

final class ConvertedComparator<T1, T2, C1, C2>
implements Comparator<T1, T2> {
    private final ComparatorInfo<C1, C2> comparator;
    @Nullable
    private final ConverterInfo<T1, C1> firstConverter;
    @Nullable
    private final ConverterInfo<T2, C2> secondConverter;

    @Contract(value="null, _, null -> fail")
    ConvertedComparator(@Nullable ConverterInfo<T1, C1> firstConverter, ComparatorInfo<C1, C2> c, @Nullable ConverterInfo<T2, C2> secondConverter) {
        if (firstConverter == null && secondConverter == null) {
            throw new IllegalArgumentException("firstConverter and secondConverter must not BOTH be null!");
        }
        this.firstConverter = firstConverter;
        this.comparator = c;
        this.secondConverter = secondConverter;
    }

    @Override
    public Relation compare(T1 o1, T2 o2) {
        T2 t2;
        T1 t1;
        Object object = t1 = this.firstConverter == null ? o1 : this.firstConverter.getConverter().convert(o1);
        if (t1 == null) {
            return Relation.NOT_EQUAL;
        }
        Object object2 = t2 = this.secondConverter == null ? o2 : this.secondConverter.getConverter().convert(o2);
        if (t2 == null) {
            return Relation.NOT_EQUAL;
        }
        return this.comparator.getComparator().compare(t1, t2);
    }

    @Override
    public boolean supportsOrdering() {
        return this.comparator.getComparator().supportsOrdering();
    }

    public String toString() {
        return "ConvertedComparator{first=" + String.valueOf(this.firstConverter) + ",comparator=" + String.valueOf(this.comparator) + ",second=" + String.valueOf(this.secondConverter) + "}";
    }
}

