/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.lang.converter;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.util.Pair;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.lang.converter.ChainedConverter;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.ConverterInfo;

public final class Converters {
    private static final List<ConverterInfo<?, ?>> CONVERTERS = new ArrayList(50);
    private static final Map<Pair<Class<?>, Class<?>>, ConverterInfo<?, ?>> QUICK_ACCESS_CONVERTERS = new HashMap(50);

    private Converters() {
    }

    public static @Unmodifiable List<ConverterInfo<?, ?>> getConverterInfos() {
        Converters.assertIsDoneLoading();
        return Collections.unmodifiableList(CONVERTERS);
    }

    public static <F, T> void registerConverter(Class<F> fromType, Class<T> toType, Converter<F, T> converter) {
        Converters.registerConverter(fromType, toType, converter, 0);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <F, T> void registerConverter(Class<F> fromType, Class<T> toType, Converter<F, T> converter, int flags) {
        Skript.checkAcceptRegistrations();
        ConverterInfo<F, T> info = new ConverterInfo<F, T>(fromType, toType, converter, flags);
        List<ConverterInfo<?, ?>> list = CONVERTERS;
        synchronized (list) {
            if (Converters.exactConverterExists_i(fromType, toType)) {
                throw new SkriptAPIException("A Converter from '" + String.valueOf(fromType) + "' to '" + String.valueOf(toType) + "' already exists!");
            }
            CONVERTERS.add(info);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <F, M, T> void createChainedConverters() {
        Skript.checkAcceptRegistrations();
        List<ConverterInfo<?, ?>> list = CONVERTERS;
        synchronized (list) {
            for (int i = 0; i < CONVERTERS.size(); ++i) {
                ConverterInfo<?, ?> unknownInfo1 = CONVERTERS.get(i);
                for (int j = 0; j < CONVERTERS.size(); ++j) {
                    ConverterInfo<?, ?> info2;
                    ConverterInfo<?, ?> info1;
                    ConverterInfo<?, ?> unknownInfo2 = CONVERTERS.get(j);
                    if (unknownInfo2.getFrom() != Object.class && unknownInfo1.getFrom() != unknownInfo2.getTo() && (unknownInfo1.getFlags() & 2) == 0 && (unknownInfo2.getFlags() & 1) == 0 && unknownInfo2.getFrom().isAssignableFrom(unknownInfo1.getTo()) && !Converters.exactConverterExists_i(unknownInfo1.getFrom(), unknownInfo2.getTo())) {
                        info1 = unknownInfo1;
                        info2 = unknownInfo2;
                        CONVERTERS.add(new ConverterInfo(info1.getFrom(), info2.getTo(), new ChainedConverter(info1, info2), info1.getFlags() | info2.getFlags()));
                        continue;
                    }
                    if (unknownInfo1.getFrom() == Object.class || unknownInfo2.getFrom() == unknownInfo1.getTo() || (unknownInfo1.getFlags() & 1) != 0 || (unknownInfo2.getFlags() & 2) != 0 || !unknownInfo1.getFrom().isAssignableFrom(unknownInfo2.getTo()) || Converters.exactConverterExists_i(unknownInfo2.getFrom(), unknownInfo1.getTo())) continue;
                    info1 = unknownInfo1;
                    info2 = unknownInfo2;
                    CONVERTERS.add(new ConverterInfo(info2.getFrom(), info1.getTo(), new ChainedConverter(info2, info1), info2.getFlags() | info1.getFlags()));
                }
            }
        }
    }

    private static boolean exactConverterExists_i(Class<?> fromType, Class<?> toType) {
        for (ConverterInfo<?, ?> info : CONVERTERS) {
            if (fromType != info.getFrom() || toType != info.getTo()) continue;
            return true;
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean exactConverterExists(Class<?> fromType, Class<?> toType) {
        List<ConverterInfo<?, ?>> list = CONVERTERS;
        synchronized (list) {
            return Converters.exactConverterExists_i(fromType, toType);
        }
    }

    public static boolean converterExists(Class<?> fromType, Class<?> toType) {
        Converters.assertIsDoneLoading();
        if (toType.isAssignableFrom(fromType)) {
            return true;
        }
        return Converters.getConverter(fromType, toType) != null;
    }

    public static boolean converterExists(Class<?> fromType, Class<?> ... toTypes) {
        Converters.assertIsDoneLoading();
        for (Class<?> toType : toTypes) {
            if (!Converters.converterExists(fromType, toType)) continue;
            return true;
        }
        return false;
    }

    @Nullable
    public static <F, T> Converter<F, T> getConverter(Class<F> fromType, Class<T> toType) {
        ConverterInfo<F, T> info = Converters.getConverterInfo(fromType, toType);
        return info != null ? info.getConverter() : null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public static <F, T> ConverterInfo<F, T> getConverterInfo(Class<F> fromType, Class<T> toType) {
        ConverterInfo<Object, Object> converter;
        Converters.assertIsDoneLoading();
        Pair<Class<F>, Class<T>> pair = new Pair<Class<F>, Class<T>>(fromType, toType);
        Map<Pair<Class<?>, Class<?>>, ConverterInfo<?, ?>> map = QUICK_ACCESS_CONVERTERS;
        synchronized (map) {
            if (QUICK_ACCESS_CONVERTERS.containsKey(pair)) {
                converter = QUICK_ACCESS_CONVERTERS.get(pair);
            } else {
                converter = Converters.getConverterInfo_i(fromType, toType);
                QUICK_ACCESS_CONVERTERS.put(pair, converter);
            }
        }
        return converter;
    }

    @Nullable
    private static <F, T extends ParentType, SubType extends F, ParentType> ConverterInfo<F, T> getConverterInfo_i(Class<F> fromType, Class<T> toType) {
        for (ConverterInfo<?, ?> info : CONVERTERS) {
            if (fromType != info.getFrom() || toType != info.getTo()) continue;
            return info;
        }
        for (ConverterInfo<?, ?> info : CONVERTERS) {
            if (!info.getFrom().isAssignableFrom(fromType) || !toType.isAssignableFrom(info.getTo())) continue;
            return info;
        }
        if (fromType == Object.class) {
            return new ConverterInfo<Object, Object>(fromType, toType, fromObject -> Converters.convert(fromObject, toType), 1);
        }
        for (ConverterInfo<?, ?> unknownInfo : CONVERTERS) {
            ConverterInfo<?, ?> info;
            int flags = unknownInfo.getFlags();
            if (unknownInfo.getFrom().isAssignableFrom(fromType) && unknownInfo.getTo().isAssignableFrom(toType)) {
                info = unknownInfo;
                if ((flags & 4) == 0 && (flags & 2) == 2) continue;
                return new ConverterInfo<Object, Object>(fromType, toType, fromObject -> {
                    Object converted = info.getConverter().convert(fromObject);
                    if (toType.isInstance(converted)) {
                        return converted;
                    }
                    return null;
                }, 0);
            }
            if (!fromType.isAssignableFrom(unknownInfo.getFrom()) || !toType.isAssignableFrom(unknownInfo.getTo())) continue;
            info = unknownInfo;
            if ((flags & 4) == 0 && (flags & 1) == 1) continue;
            return new ConverterInfo<Object, Object>(fromType, toType, fromObject -> {
                if (!info.getFrom().isInstance(fromObject)) {
                    return null;
                }
                return info.getConverter().convert(fromObject);
            }, 0);
        }
        for (ConverterInfo<?, ?> unknownInfo : CONVERTERS) {
            if (!fromType.isAssignableFrom(unknownInfo.getFrom()) || !unknownInfo.getTo().isAssignableFrom(toType)) continue;
            ConverterInfo<?, ?> info = unknownInfo;
            int flags = unknownInfo.getFlags();
            if ((flags & 4) == 0 && ((flags & 1) == 1 || (flags & 2) == 2)) continue;
            return new ConverterInfo<Object, Object>(fromType, toType, fromObject -> {
                if (!info.getFrom().isInstance(fromObject)) {
                    return null;
                }
                Object converted = info.getConverter().convert(fromObject);
                if (toType.isInstance(converted)) {
                    return converted;
                }
                return null;
            }, 0);
        }
        return null;
    }

    @Nullable
    public static <From, To> To convert(@Nullable From from, Class<To> toType) {
        Converters.assertIsDoneLoading();
        if (from == null) {
            return null;
        }
        if (toType.isInstance(from)) {
            return (To)from;
        }
        Converter<?, To> converter = Converters.getConverter(from.getClass(), toType);
        if (converter == null) {
            return null;
        }
        return converter.convert(from);
    }

    /*
     * WARNING - void declaration
     */
    @Nullable
    public static <From, To> To convert(@Nullable From from, Class<? extends To>[] toTypes) {
        void var5_8;
        Converters.assertIsDoneLoading();
        if (from == null) {
            return null;
        }
        for (Class<To> clazz : toTypes) {
            if (!clazz.isInstance(from)) continue;
            return (To)from;
        }
        Class<?> fromType = from.getClass();
        Class<? extends To>[] classArray = toTypes;
        int n = classArray.length;
        boolean bl = false;
        while (var5_8 < n) {
            Class<To> clazz = classArray[var5_8];
            Converter<?, To> converter = Converters.getConverter(fromType, clazz);
            if (converter != null) {
                return converter.convert(from);
            }
            ++var5_8;
        }
        return null;
    }

    public static <From, To> void convert(@Nullable From[] from, To[] destination, Class<? extends To>[] toTypes) {
        Converters.assertIsDoneLoading();
        int length = Math.min(from.length, destination.length);
        if (length == 0) {
            return;
        }
        for (int i = 0; i < length; ++i) {
            destination[i] = from[i] != null ? Converters.convert(from[i], toTypes) : null;
        }
    }

    public static <To> To[] convert(Object @Nullable [] from, Class<To> toType) {
        Converters.assertIsDoneLoading();
        if (from == null) {
            return (Object[])Array.newInstance(toType, 0);
        }
        if (toType.isAssignableFrom(from.getClass().getComponentType())) {
            return from;
        }
        ArrayList<To> converted = new ArrayList<To>(from.length);
        for (Object fromSingle : from) {
            To convertedSingle = Converters.convert(fromSingle, toType);
            if (convertedSingle == null) continue;
            converted.add(convertedSingle);
        }
        return converted.toArray((Object[])Array.newInstance(toType, converted.size()));
    }

    /*
     * WARNING - void declaration
     */
    public static <To> To[] convert(Object @Nullable [] from, Class<? extends To>[] toTypes, Class<To> superType) {
        void var7_10;
        Converters.assertIsDoneLoading();
        if (from == null) {
            return (Object[])Array.newInstance(superType, 0);
        }
        Class<?> fromType = from.getClass().getComponentType();
        for (Class<To> clazz : toTypes) {
            if (!clazz.isAssignableFrom(fromType)) continue;
            return from;
        }
        ArrayList<To> converted = new ArrayList<To>(from.length);
        Object[] objectArray = from;
        int n = objectArray.length;
        boolean bl = false;
        while (var7_10 < n) {
            Object fromSingle = objectArray[var7_10];
            To convertedSingle = Converters.convert(fromSingle, toTypes);
            if (convertedSingle != null) {
                converted.add(convertedSingle);
            }
            ++var7_10;
        }
        return converted.toArray((Object[])Array.newInstance(superType, converted.size()));
    }

    public static <From, To> To[] convert(From[] from, Class<To> toType, Converter<? super From, ? extends To> converter) {
        Converters.assertIsDoneLoading();
        Object[] converted = (Object[])Array.newInstance(toType, from.length);
        int j = 0;
        for (From fromSingle : from) {
            Object convertedSingle;
            Object v0 = convertedSingle = fromSingle == null ? null : converter.convert((From)fromSingle);
            if (convertedSingle == null) continue;
            converted[j++] = convertedSingle;
        }
        if (j != converted.length) {
            converted = Arrays.copyOf(converted, j);
        }
        return converted;
    }

    public static <From, To> void convert(From[] from, To[] destination, Converter<? super From, ? extends To> converter) {
        Converters.assertIsDoneLoading();
        int length = Math.min(from.length, destination.length);
        if (length == 0) {
            return;
        }
        for (int i = 0; i < length; ++i) {
            destination[i] = from[i] != null ? converter.convert((From)from[i]) : null;
        }
    }

    public static <To> To convertStrictly(Object from, Class<To> toType) {
        To converted = Converters.convert(from, toType);
        if (converted == null) {
            throw new ClassCastException("Cannot convert '" + String.valueOf(from) + "' to an object of type '" + String.valueOf(toType) + "'");
        }
        return converted;
    }

    public static <To> To[] convertStrictly(Object[] from, Class<To> toType) {
        Converters.assertIsDoneLoading();
        Object[] converted = (Object[])Array.newInstance(toType, from.length);
        for (int i = 0; i < from.length; ++i) {
            To convertedSingle = Converters.convert(from[i], toType);
            if (convertedSingle == null) {
                throw new ClassCastException("Cannot convert '" + String.valueOf(from[i]) + "' to an object of type '" + String.valueOf(toType) + "'");
            }
            converted[i] = convertedSingle;
        }
        return converted;
    }

    public static <From, To> To[] convertUnsafe(From[] from, Class<?> toType, Converter<? super From, ? extends To> converter) {
        return Converters.convert(from, toType, converter);
    }

    private static void assertIsDoneLoading() {
        if (Skript.isAcceptRegistrations()) {
            throw new SkriptAPIException("Converters cannot be retrieved until Skript has finished registrations.");
        }
    }
}

