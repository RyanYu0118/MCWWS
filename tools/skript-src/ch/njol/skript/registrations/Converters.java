/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.registrations;

import ch.njol.skript.util.Utils;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.ConverterInfo;

@Deprecated(since="2.10.0", forRemoval=true)
public abstract class Converters {
    @Deprecated(since="2.10.0", forRemoval=true)
    public static <F, T> List<ConverterInfo<?, ?>> getConverters() {
        return org.skriptlang.skript.lang.converter.Converters.getConverterInfos().stream().map(unknownInfo -> {
            ConverterInfo info = unknownInfo;
            return new ConverterInfo(info.getFrom(), info.getTo(), info.getConverter(), info.getFlags());
        }).collect(Collectors.toList());
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static <F, T> void registerConverter(Class<F> from, Class<T> to, ch.njol.skript.classes.Converter<F, T> converter) {
        Converters.registerConverter(from, to, converter, 0);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static <F, T> void registerConverter(Class<F> from, Class<T> to, ch.njol.skript.classes.Converter<F, T> converter, int options) {
        org.skriptlang.skript.lang.converter.Converters.registerConverter(from, to, converter::convert, options);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static <F, T> T convert(@Nullable F o, Class<T> to) {
        return org.skriptlang.skript.lang.converter.Converters.convert(o, to);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static <F, T> T convert(@Nullable F o, Class<? extends T>[] to) {
        return org.skriptlang.skript.lang.converter.Converters.convert(o, to);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static <T> T[] convertArray(@Nullable Object[] o, Class<T> to) {
        T[] converted = org.skriptlang.skript.lang.converter.Converters.convert(o, to);
        if (converted.length == 0) {
            return null;
        }
        return converted;
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static <T> T[] convertArray(@Nullable Object[] o, Class<? extends T>[] to, Class<T> superType) {
        return org.skriptlang.skript.lang.converter.Converters.convert(o, to, superType);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static <T> T[] convertStrictly(Object[] original, Class<T> to) throws ClassCastException {
        return org.skriptlang.skript.lang.converter.Converters.convertStrictly(original, to);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static <T> T convertStrictly(Object original, Class<T> to) throws ClassCastException {
        return org.skriptlang.skript.lang.converter.Converters.convertStrictly(original, to);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static boolean converterExists(Class<?> from, Class<?> to) {
        return org.skriptlang.skript.lang.converter.Converters.converterExists(from, to);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static boolean converterExists(Class<?> from, Class<?> ... to) {
        return org.skriptlang.skript.lang.converter.Converters.converterExists(from, to);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static <F, T> ch.njol.skript.classes.Converter<? super F, ? extends T> getConverter(Class<F> from, Class<T> to) {
        Converter<F, T> converter = org.skriptlang.skript.lang.converter.Converters.getConverter(from, to);
        if (converter == null) {
            return null;
        }
        return converter::convert;
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static <F, T> ConverterInfo<? super F, ? extends T> getConverterInfo(Class<F> from, Class<T> to) {
        ConverterInfo<F, T> info = org.skriptlang.skript.lang.converter.Converters.getConverterInfo(from, to);
        if (info == null) {
            return null;
        }
        return new ConverterInfo<Object, Object>(info.getFrom(), info.getTo(), info.getConverter()::convert, info.getFlags());
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static <F, T> T[] convertUnsafe(F[] from, Class<?> to, ch.njol.skript.classes.Converter<? super F, ? extends T> conv) {
        return org.skriptlang.skript.lang.converter.Converters.convertUnsafe(from, to, conv::convert);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static <F, T> T[] convert(F[] from, Class<T> to, ch.njol.skript.classes.Converter<? super F, ? extends T> conv) {
        return org.skriptlang.skript.lang.converter.Converters.convert(from, to, conv::convert);
    }

    static {
        Utils.loadedRemovedClassWarning(Converters.class);
    }
}

