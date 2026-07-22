/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.ApiStatus$NonExtendable
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.common.function;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.registrations.Classes;
import com.google.common.base.Preconditions;
import java.util.Set;
import java.util.StringJoiner;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.Converters;

@ApiStatus.NonExtendable
public interface Parameter<T> {
    @NotNull
    public String name();

    @NotNull
    public Class<T> type();

    public @Unmodifiable @NotNull Set<Modifier> modifiers();

    default public boolean hasModifier(Modifier modifier) {
        return this.modifiers().contains(modifier);
    }

    default public <M extends Modifier> M getModifier(Class<M> modifierClass) {
        return (M)((Modifier)this.modifiers().stream().filter(modifierClass::isInstance).map(modifierClass::cast).findFirst().orElse(null));
    }

    default public Object[] evaluate(@Nullable Expression<? extends T> argument, Event event) {
        if (argument == null) {
            return null;
        }
        Object[] values = argument.getArray(event);
        for (int i = 0; i < values.length; ++i) {
            values[i] = Classes.clone(values[i]);
        }
        if (!this.hasModifier(Modifier.KEYED)) {
            return values;
        }
        String[] keys = KeyProviderExpression.areKeysRecommended(argument) ? ((KeyProviderExpression)argument).getArrayKeys(event) : null;
        return KeyedValue.zip(values, keys);
    }

    default public boolean isSingle() {
        return !this.type().isArray();
    }

    @Deprecated(forRemoval=true, since="2.14")
    default public boolean single() {
        return this.isSingle();
    }

    default public String toFormattedString() {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add("%s:".formatted(this.name()));
        if (this.hasModifier(Modifier.OPTIONAL)) {
            joiner.add("optional");
        }
        Noun exact = Classes.getSuperClassInfo(this.type()).getName();
        if (this.type().isArray()) {
            joiner.add(exact.getPlural());
        } else {
            joiner.add(exact.getSingular());
        }
        if (this.hasModifier(Modifier.RANGED)) {
            Modifier.RangedModifier range = this.getModifier(Modifier.RangedModifier.class);
            joiner.add("between").add(range.getMin().toString()).add("and").add(range.getMax().toString());
        }
        return joiner.toString();
    }

    public static interface Modifier {
        public static final Modifier OPTIONAL = Modifier.of();
        public static final Modifier KEYED = Modifier.of();
        public static final Modifier RANGED = new RangedModifier<Integer>(0, 0);

        public static Modifier of() {
            return new Modifier(){};
        }

        public static <T extends Comparable<T>> RangedModifier<T> ranged(T min, T max) {
            return new RangedModifier<T>(min, max);
        }

        public static class RangedModifier<T extends Comparable<T>>
        implements Modifier {
            private final T min;
            private final T max;

            private RangedModifier(T min, T max) {
                Preconditions.checkState((min.compareTo(max) < 1 ? 1 : 0) != 0, (Object)"Min value cannot be greater than max value!");
                this.min = min;
                this.max = max;
            }

            public T getMin() {
                return this.min;
            }

            public T getMax() {
                return this.max;
            }

            public boolean inRange(Object input) {
                if (!this.min.getClass().isInstance(input)) {
                    Converter<?, ?> converter = Converters.getConverter(input.getClass(), this.min.getClass());
                    if (converter == null) {
                        return false;
                    }
                    if ((input = converter.convert(input)) == null) {
                        return false;
                    }
                }
                return ((Comparable)input).compareTo(this.min) > -1 && ((Comparable)input).compareTo(this.max) < 1;
            }

            public boolean inRange(Object @NotNull [] inputs) {
                if (inputs.length == 0) {
                    return false;
                }
                for (Object input : inputs) {
                    if (this.inRange(input)) continue;
                    return false;
                }
                return true;
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public boolean equals(Object obj) {
                if (obj == RANGED) return true;
                if (!(obj instanceof RangedModifier)) return false;
                RangedModifier range = (RangedModifier)obj;
                if (this == RANGED) return true;
                if (range.max != this.max) return false;
                if (range.min != this.min) return false;
                return true;
            }

            public int hashCode() {
                return 439824729;
            }

            public String toString() {
                return "RangedModifier(min=" + String.valueOf(this.min) + ", max=" + String.valueOf(this.max) + ")";
            }
        }
    }
}

