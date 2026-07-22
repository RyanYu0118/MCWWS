/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.lang.comparator;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.lang.comparator.Comparator;
import org.skriptlang.skript.lang.comparator.ComparatorInfo;
import org.skriptlang.skript.lang.comparator.ConvertedComparator;
import org.skriptlang.skript.lang.comparator.InverseComparator;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.lang.converter.ConverterInfo;
import org.skriptlang.skript.lang.converter.Converters;

public final class Comparators {
    private static final ComparatorInfo<Object, Object> EQUALS_COMPARATOR_INFO = new ComparatorInfo<Object, Object>(Object.class, Object.class, (o1, o2) -> Relation.get(o1.equals(o2)));
    private static final List<ComparatorInfo<?, ?>> COMPARATORS = new ArrayList(50);
    private static final Map<Pair<Class<?>, Class<?>>, ComparatorInfo<?, ?>> QUICK_ACCESS_COMPARATORS = new HashMap(50);

    private Comparators() {
    }

    public static @Unmodifiable List<ComparatorInfo<?, ?>> getComparatorInfos() {
        Comparators.assertIsDoneLoading();
        return Collections.unmodifiableList(COMPARATORS);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <T1, T2> void registerComparator(Class<T1> firstType, Class<T2> secondType, Comparator<T1, T2> comparator) {
        Skript.checkAcceptRegistrations();
        if (firstType == Object.class && secondType == Object.class) {
            throw new IllegalArgumentException("It is not possible to add a comparator between objects");
        }
        List<ComparatorInfo<?, ?>> list = COMPARATORS;
        synchronized (list) {
            if (Comparators.exactComparatorExists_i(firstType, secondType)) {
                throw new SkriptAPIException("A Comparator comparing '" + String.valueOf(firstType) + "' and '" + String.valueOf(secondType) + "' already exists!");
            }
            COMPARATORS.add(new ComparatorInfo<T1, T2>(firstType, secondType, comparator));
        }
    }

    private static boolean exactComparatorExists_i(Class<?> firstType, Class<?> secondType) {
        for (ComparatorInfo<?, ?> info : COMPARATORS) {
            if (info.getFirstType() != firstType || info.getSecondType() != secondType) continue;
            return true;
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean exactComparatorExists(Class<?> firstType, Class<?> secondType) {
        List<ComparatorInfo<?, ?>> list = COMPARATORS;
        synchronized (list) {
            return Comparators.exactComparatorExists_i(firstType, secondType);
        }
    }

    public static boolean comparatorExists(Class<?> firstType, Class<?> secondType) {
        Comparators.assertIsDoneLoading();
        if (firstType != Object.class && firstType == secondType) {
            return true;
        }
        return Comparators.getComparator(firstType, secondType) != null;
    }

    public static <T1, T2> Relation compare(@Nullable T1 first, @Nullable T2 second) {
        Comparators.assertIsDoneLoading();
        if (first == null || second == null) {
            return Relation.NOT_EQUAL;
        }
        if (first == second) {
            return Relation.EQUAL;
        }
        Comparator<?, ?> comparator = Comparators.getComparator(first.getClass(), second.getClass());
        if (comparator == null) {
            return Relation.NOT_EQUAL;
        }
        return comparator.compare(first, second);
    }

    @Nullable
    public static <T1, T2> Comparator<T1, T2> getComparator(Class<T1> firstType, Class<T2> secondType) {
        ComparatorInfo<T1, T2> info = Comparators.getComparatorInfo(firstType, secondType);
        return info != null ? info.getComparator() : null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public static <T1, T2> ComparatorInfo<T1, T2> getComparatorInfo(Class<T1> firstType, Class<T2> secondType) {
        ComparatorInfo<Object, Object> comparator;
        Comparators.assertIsDoneLoading();
        Pair<Class<T1>, Class<T2>> pair = new Pair<Class<T1>, Class<T2>>(firstType, secondType);
        Map<Pair<Class<?>, Class<?>>, ComparatorInfo<?, ?>> map = QUICK_ACCESS_COMPARATORS;
        synchronized (map) {
            if (QUICK_ACCESS_COMPARATORS.containsKey(pair)) {
                comparator = QUICK_ACCESS_COMPARATORS.get(pair);
            } else {
                comparator = Comparators.getComparatorInfo_i(firstType, secondType);
                QUICK_ACCESS_COMPARATORS.put(pair, comparator);
            }
        }
        return comparator;
    }

    @Nullable
    private static <T1, T2, C1, C2> ComparatorInfo<T1, T2> getComparatorInfo_i(Class<T1> firstType, Class<T2> secondType) {
        ConverterInfo<T2, ?> c2;
        ConverterInfo<T1, ?> c1;
        ComparatorInfo<?, ?> info;
        for (ComparatorInfo<?, ?> info2 : COMPARATORS) {
            if (info2.getFirstType() != firstType || info2.getSecondType() != secondType) continue;
            return info2;
        }
        for (ComparatorInfo<?, ?> info2 : COMPARATORS) {
            if (!info2.getFirstType().isAssignableFrom(firstType) || !info2.getSecondType().isAssignableFrom(secondType)) continue;
            return info2;
        }
        for (ComparatorInfo<?, ?> info2 : COMPARATORS) {
            if (!info2.getComparator().supportsInversion() || !info2.getFirstType().isAssignableFrom(secondType) || !info2.getSecondType().isAssignableFrom(firstType)) continue;
            return new ComparatorInfo<T1, T2>(firstType, secondType, new InverseComparator(info2));
        }
        if (Utils.getSuperType(firstType, secondType) == Object.class) {
            ConverterInfo<T1, T2> fs = Converters.getConverterInfo(firstType, secondType);
            if (fs != null) {
                return new ComparatorInfo<T1, T2>(firstType, secondType, new ConvertedComparator(fs, Comparators.getComparatorInfo(secondType, secondType), null));
            }
            ConverterInfo<T2, T1> sf = Converters.getConverterInfo(secondType, firstType);
            if (sf != null) {
                return new ComparatorInfo<T1, T2>(firstType, secondType, new ConvertedComparator(null, Comparators.getComparatorInfo(firstType, firstType), sf));
            }
        }
        for (ComparatorInfo<?, ?> unknownInfo : COMPARATORS) {
            ConverterInfo<T1, ?> fc1;
            ConverterInfo<T2, ?> sc2;
            info = unknownInfo;
            if (info.getFirstType().isAssignableFrom(firstType) && (sc2 = Converters.getConverterInfo(secondType, info.getSecondType())) != null) {
                return new ComparatorInfo<T1, T2>(firstType, secondType, new ConvertedComparator(null, info, sc2));
            }
            if (!info.getSecondType().isAssignableFrom(secondType) || (fc1 = Converters.getConverterInfo(firstType, info.getFirstType())) == null) continue;
            return new ComparatorInfo<T1, T2>(firstType, secondType, new ConvertedComparator(fc1, info, null));
        }
        for (ComparatorInfo<?, ?> unknownInfo : COMPARATORS) {
            ConverterInfo<T1, ?> fc2;
            ConverterInfo<T2, ?> sc1;
            if (!unknownInfo.getComparator().supportsInversion()) continue;
            info = unknownInfo;
            if (info.getSecondType().isAssignableFrom(firstType) && (sc1 = Converters.getConverterInfo(secondType, info.getFirstType())) != null) {
                return new ComparatorInfo<T1, T2>(firstType, secondType, new InverseComparator<T1, T2>(new ComparatorInfo<T2, T1>(secondType, firstType, new ConvertedComparator(sc1, info, null))));
            }
            if (!info.getFirstType().isAssignableFrom(secondType) || (fc2 = Converters.getConverterInfo(firstType, info.getSecondType())) == null) continue;
            return new ComparatorInfo<T1, T2>(firstType, secondType, new InverseComparator<T1, T2>(new ComparatorInfo<T2, T1>(secondType, firstType, new ConvertedComparator(null, info, fc2))));
        }
        Iterator<ComparatorInfo<?, ?>> iterator = COMPARATORS.iterator();
        while (iterator.hasNext()) {
            ComparatorInfo<?, ?> unknownInfo;
            info = unknownInfo = iterator.next();
            c1 = Converters.getConverterInfo(firstType, info.getFirstType());
            c2 = Converters.getConverterInfo(secondType, info.getSecondType());
            if (c1 == null || c2 == null) continue;
            return new ComparatorInfo<T1, T2>(firstType, secondType, new ConvertedComparator(c1, info, c2));
        }
        for (ComparatorInfo<?, ?> unknownInfo : COMPARATORS) {
            if (!unknownInfo.getComparator().supportsInversion()) continue;
            info = unknownInfo;
            c1 = Converters.getConverterInfo(firstType, info.getSecondType());
            c2 = Converters.getConverterInfo(secondType, info.getFirstType());
            if (c1 == null || c2 == null) continue;
            return new ComparatorInfo<T1, T2>(firstType, secondType, new InverseComparator<T1, T2>(new ComparatorInfo<T2, T1>(secondType, firstType, new ConvertedComparator(c2, info, c1))));
        }
        if (firstType != Object.class && Classes.getSuperClassInfo(firstType) == Classes.getSuperClassInfo(secondType)) {
            return EQUALS_COMPARATOR_INFO;
        }
        return null;
    }

    private static void assertIsDoneLoading() {
        if (Skript.isAcceptRegistrations()) {
            throw new SkriptAPIException("Comparators cannot be retrieved until Skript has finished registrations.");
        }
    }
}

