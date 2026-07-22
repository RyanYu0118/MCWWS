/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record KeyedValue<T>(@NotNull String key, @NotNull T value) implements Map.Entry<String, T>
{
    public KeyedValue(@NotNull String key, @NotNull T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
    }

    public KeyedValue(Map.Entry<String, T> entry) {
        this(entry.getKey(), entry.getValue());
    }

    @Override
    public String getKey() {
        return this.key();
    }

    @Override
    public T getValue() {
        return this.value();
    }

    @Override
    public T setValue(T value) {
        throw new UnsupportedOperationException("KeyedValue is immutable, cannot set value");
    }

    public KeyedValue<T> withKey(@NotNull String newKey) {
        return new KeyedValue<T>(newKey, this.value());
    }

    public <U> KeyedValue<U> withValue(@NotNull U newValue) {
        return new KeyedValue<U>(this.key(), newValue);
    }

    public static <T, U> KeyedValue<U>[] map(KeyedValue<T>[] source, Function<T, @Nullable U> mapper) {
        if (source == null) {
            return new KeyedValue[0];
        }
        KeyedValue[] mapped = new KeyedValue[source.length];
        for (int i = 0; i < source.length; ++i) {
            if (source[i] == null) continue;
            U mappedValue = mapper.apply(source[i].value());
            mapped[i] = mappedValue != null ? source[i].withValue(mappedValue) : null;
        }
        return mapped;
    }

    public static <T> KeyedValue<T> @NotNull [] zip(@NotNull @NotNull T @NotNull [] values, @NotNull String @Nullable [] keys) {
        if (keys == null) {
            KeyedValue[] keyedValues = new KeyedValue[values.length];
            for (int i = 0; i < values.length; ++i) {
                keyedValues[i] = new KeyedValue<T>(String.valueOf(i + 1), values[i]);
            }
            return keyedValues;
        }
        if (values.length != keys.length) {
            throw new IllegalArgumentException("Values and keys must have the same length");
        }
        KeyedValue[] keyedValues = new KeyedValue[values.length];
        for (int i = 0; i < values.length; ++i) {
            keyedValues[i] = new KeyedValue<T>(keys[i], values[i]);
        }
        return keyedValues;
    }

    public static <T> UnzippedKeyValues<T> unzip(@NotNull @NotNull KeyedValue<T> @NotNull [] keyedValues) {
        ArrayList<String> keys = new ArrayList<String>(keyedValues.length);
        ArrayList<T> values = new ArrayList<T>(keyedValues.length);
        for (KeyedValue<T> keyedValue : keyedValues) {
            keys.add(keyedValue.key());
            values.add(keyedValue.value());
        }
        return new UnzippedKeyValues(keys, values);
    }

    public static <T> UnzippedKeyValues<T> unzip(Iterator<KeyedValue<T>> keyedValues) {
        ArrayList<String> keys = new ArrayList<String>();
        ArrayList<T> values = new ArrayList<T>();
        while (keyedValues.hasNext()) {
            KeyedValue<T> keyedValue = keyedValues.next();
            keys.add(keyedValue.key());
            values.add(keyedValue.value());
        }
        return new UnzippedKeyValues(keys, values);
    }

    public record UnzippedKeyValues<T>(@NotNull @NotNull List<@NotNull String> keys, @NotNull @NotNull List<@NotNull T> values) {
        public UnzippedKeyValues(@Nullable List<@NotNull String> keys, @NotNull @NotNull List<@NotNull T> values) {
            this.values = Objects.requireNonNull(values, "values");
            ArrayList arrayList = this.keys = keys != null ? keys : new ArrayList(values.size());
            if (keys == null) {
                for (int i = 1; i <= values.size(); ++i) {
                    this.keys.add(String.valueOf(i));
                }
            } else if (keys.size() != values.size()) {
                throw new IllegalArgumentException("Keys and values must have the same length");
            }
        }

        public UnzippedKeyValues(@NotNull String @Nullable [] keys, @NotNull @NotNull T @NotNull [] values) {
            this(keys != null ? Arrays.asList(keys) : null, Arrays.asList(values));
        }
    }
}

