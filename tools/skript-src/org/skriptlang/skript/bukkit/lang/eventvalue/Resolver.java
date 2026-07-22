/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.lang.eventvalue;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.util.ClassUtils;

class Resolver<E extends Event, V> {
    static final EventComparatorFactory EVENT_DISTANCE_COMPARATOR = eventClass -> Comparator.comparingInt(ev -> ClassUtils.hierarchyDistance(ev.eventClass(), eventClass));
    static final EventComparatorFactory BI_EVENT_DISTANCE_COMPARATOR = eventClass -> EVENT_DISTANCE_COMPARATOR.create(eventClass).thenComparingInt(ev -> ClassUtils.hierarchyDistance(eventClass, ev.valueClass()));
    static final EventValueComparatorFactory EVENT_VALUE_DISTANCE_COMPARATOR = (eventClass, valueClass) -> BI_EVENT_DISTANCE_COMPARATOR.create(eventClass).thenComparingInt(ev -> ClassUtils.hierarchyDistance(valueClass, ev.valueClass()));
    private final Class<E> eventClass;
    @Nullable
    private final Class<V> valueClass;
    private final Predicate<EventValue<?, ?>> filter;
    private final Comparator<EventValue<?, ?>> comparator;
    private final Function<EventValue<?, ?>, @Nullable EventValue<E, V>> mapper;
    private final boolean filterMatches;

    private Resolver(Class<E> eventClass, @Nullable Class<V> valueClass, Predicate<EventValue<?, ?>> filter, Comparator<EventValue<?, ?>> comparator, Function<EventValue<?, ?>, @Nullable EventValue<E, V>> mapper, boolean filterMatches) {
        this.eventClass = eventClass;
        this.valueClass = valueClass;
        this.filter = filter;
        this.comparator = comparator;
        this.mapper = mapper;
        this.filterMatches = filterMatches;
    }

    public EventValueRegistry.Resolution<E, V> resolve(List<EventValue<?, ?>> eventValues) {
        ArrayList best = new ArrayList();
        EventValue<E, E> bestMatch = null;
        block4: for (EventValue<E, E> eventValue : eventValues) {
            EventValue<E, E> converted;
            int comparison;
            if (!this.filter.test(eventValue)) continue;
            switch (eventValue.validate(this.eventClass)) {
                case INVALID: {
                    continue block4;
                }
                case ABORT: {
                    return EventValueRegistry.Resolution.error();
                }
            }
            int n = comparison = bestMatch != null ? this.comparator.compare(eventValue, bestMatch) : -1;
            if (comparison < 0) {
                EventValue<E, Object> eventValue2 = converted = this.mapper != null ? this.mapper.apply(eventValue) : eventValue;
                if (converted == null) continue;
                best.clear();
                best.add(converted);
                bestMatch = eventValue;
                continue;
            }
            if (comparison != 0) continue;
            EventValue<E, Object> eventValue3 = converted = this.mapper != null ? this.mapper.apply(eventValue) : eventValue;
            if (converted == null) continue;
            best.add(converted);
        }
        if (this.valueClass != null && this.filterMatches) {
            return EventValueRegistry.Resolution.of(Resolver.filterEventValues(this.valueClass, best));
        }
        return EventValueRegistry.Resolution.of(best);
    }

    private static <E extends Event, V> List<EventValue<E, V>> filterEventValues(Class<V> valueClass, List<EventValue<E, V>> eventValues) {
        if (eventValues.size() <= 1) {
            return eventValues;
        }
        ArrayList<EventValue<EventValue<E, V>, V>> filtered = new ArrayList<EventValue<EventValue<E, V>, V>>();
        ClassInfo<V> requestedValueClassInfo = Classes.getExactClassInfo(valueClass);
        for (EventValue<E, V> eventValue : eventValues) {
            ClassInfo<V> eventValueClassInfo = Classes.getExactClassInfo(eventValue.valueClass());
            if (eventValueClassInfo != null && !eventValueClassInfo.equals(requestedValueClassInfo)) continue;
            filtered.add(eventValue);
        }
        return filtered.isEmpty() ? eventValues : filtered;
    }

    static <E extends Event, V> Builder<E, V> builder(Class<E> eventClass) {
        return new Builder(eventClass, null);
    }

    static <E extends Event, V> Builder<E, V> builder(Class<E> eventClass, Class<V> valueClass) {
        return new Builder<E, V>(eventClass, valueClass);
    }

    static class Builder<E extends Event, V> {
        private final Class<E> eventClass;
        @Nullable
        private final Class<V> valueClass;
        private Predicate<EventValue<?, ?>> filter = ev -> true;
        private Comparator<EventValue<?, ?>> comparator = (a, b) -> 0;
        private Function<EventValue<?, ?>, @Nullable EventValue<E, V>> mapper;
        private boolean filterMatches = false;

        Builder(Class<E> eventClass, @Nullable Class<V> valueClass) {
            this.eventClass = eventClass;
            this.valueClass = valueClass;
        }

        public Builder<E, V> filter(Predicate<EventValue<?, ?>> filter) {
            this.filter = filter;
            return this;
        }

        public Builder<E, V> comparator(Comparator<EventValue<?, ?>> comparator) {
            this.comparator = comparator;
            return this;
        }

        public Builder<E, V> comparator(EventComparatorFactory factory) {
            this.comparator = factory.create(this.eventClass);
            return this;
        }

        public Builder<E, V> comparator(EventValueComparatorFactory factory) {
            this.comparator = factory.create(this.eventClass, this.valueClass);
            return this;
        }

        public Builder<E, V> mapper(Function<EventValue<?, ?>, @Nullable EventValue<E, V>> mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder<E, V> filterMatches() {
            this.filterMatches = true;
            return this;
        }

        public Resolver<E, V> build() {
            return new Resolver<E, V>(this.eventClass, this.valueClass, this.filter, this.comparator, this.mapper, this.filterMatches);
        }
    }

    @FunctionalInterface
    static interface EventComparatorFactory {
        public Comparator<EventValue<?, ?>> create(Class<? extends Event> var1);
    }

    @FunctionalInterface
    static interface EventValueComparatorFactory {
        public Comparator<EventValue<?, ?>> create(Class<? extends Event> var1, Class<?> var2);
    }
}

