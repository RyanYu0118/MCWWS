/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.lang.converter;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.converter.Converter;

public final class ConverterInfo<F, T> {
    private final Class<F> from;
    private final Class<T> to;
    private final Converter<F, T> converter;
    private final int flags;

    public ConverterInfo(@NotNull Class<F> from, @NotNull Class<T> to, @NotNull Converter<F, T> converter, int flags) {
        Preconditions.checkNotNull(from, (Object)"Cannot convert from nothing to something! (from is null)");
        Preconditions.checkNotNull(to, (Object)"Cannot convert from something to nothing! (to is null)");
        Preconditions.checkNotNull(converter, (Object)"Cannot convert using a null converter!");
        this.from = from;
        this.to = to;
        this.converter = converter;
        this.flags = flags;
    }

    public Class<F> getFrom() {
        return this.from;
    }

    public Class<T> getTo() {
        return this.to;
    }

    public Converter<F, T> getConverter() {
        return this.converter;
    }

    public int getFlags() {
        return this.flags;
    }

    public String toString() {
        return "ConverterInfo{from=" + String.valueOf(this.from) + ",to=" + String.valueOf(this.to) + ",converter=" + String.valueOf(this.converter) + ",flags=" + this.flags + "}";
    }
}

