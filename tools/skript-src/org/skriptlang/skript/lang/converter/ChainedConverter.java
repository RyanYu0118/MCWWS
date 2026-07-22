/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.converter;

import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.ConverterInfo;

final class ChainedConverter<F, M, T>
implements Converter<F, T> {
    private final ConverterInfo<F, M> first;
    private final ConverterInfo<M, T> second;

    ChainedConverter(ConverterInfo<F, M> first, ConverterInfo<M, T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    @Nullable
    public T convert(F from) {
        M middle = this.first.getConverter().convert(from);
        if (middle == null) {
            return null;
        }
        return this.second.getConverter().convert(middle);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ChainedConverter{(");
        if (this.first.getConverter() instanceof ChainedConverter) {
            builder.append(this.first.getConverter());
        } else {
            builder.append(this.first.getFrom()).append(" -> ").append(this.first.getTo());
        }
        builder.append(") -> (");
        if (this.second.getConverter() instanceof ChainedConverter) {
            builder.append(this.second.getConverter());
        } else {
            builder.append(this.second.getFrom()).append(" -> ").append(this.second.getTo());
        }
        builder.append(")}");
        return builder.toString();
    }
}

