/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.bukkit.lang.eventvalue;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.bukkit.lang.eventvalue.ConvertedEventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.lang.eventvalue.Resolver;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.util.ClassUtils;

final class EventValueRegistryImpl
implements EventValueRegistry {
    private final Skript skript;
    private final Map<EventValue.Time, List<EventValue<?, ?>>> eventValues = new EnumMap(EventValue.Time.class);
    private final transient Map<Input<?, ?>, EventValueRegistry.Resolution<?, ?>> eventValuesCache = new ConcurrentHashMap();

    public EventValueRegistryImpl(Skript skript) {
        this.skript = skript;
        for (EventValue.Time time : EventValue.Time.values()) {
            this.eventValues.put(time, new ArrayList());
        }
    }

    @Override
    public <E extends Event> void register(EventValue<E, ?> eventValue) {
        Preconditions.checkNotNull(eventValue, (Object)"eventValue");
        if (eventValue instanceof ConvertedEventValue) {
            throw new SkriptAPIException("Cannot register a converted event value: " + String.valueOf(eventValue));
        }
        if (this.isRegistered(eventValue)) {
            Skript.warning(String.valueOf(eventValue) + " is already registered.");
            return;
        }
        List<EventValue<?, ?>> eventValues = this.eventValues(eventValue.time());
        eventValues.add(eventValue);
        this.eventValuesCache.clear();
    }

    @Override
    public boolean unregister(EventValue<?, ?> eventValue) {
        boolean removed = this.eventValues(eventValue.time()).remove(eventValue);
        if (removed) {
            this.eventValuesCache.clear();
        }
        return removed;
    }

    @Override
    public boolean isRegistered(EventValue<?, ?> eventValue) {
        for (EventValue<?, ?> existing : this.eventValues(eventValue.time())) {
            if (!existing.matches(eventValue)) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean isRegistered(Class<? extends Event> eventClass, Class<?> valueClass, EventValue.Time time) {
        for (EventValue<?, ?> eventValue : this.eventValues(time)) {
            if (!eventValue.matches(eventClass, valueClass)) continue;
            return true;
        }
        return false;
    }

    @Override
    public <E extends Event, V> EventValueRegistry.Resolution<E, V> resolve(Class<E> eventClass, String identifier) {
        return this.resolve(eventClass, identifier, EventValue.Time.NOW);
    }

    @Override
    public <E extends Event, V> EventValueRegistry.Resolution<E, V> resolve(Class<E> eventClass, String identifier, EventValue.Time time) {
        return this.resolve(eventClass, identifier, time, EventValueRegistry.Flags.DEFAULT);
    }

    @Override
    public <E extends Event, V> EventValueRegistry.Resolution<E, V> resolve(Class<E> eventClass, String identifier, EventValue.Time time, EventValueRegistry.Flags flags) {
        Input<E, String> input;
        EventValueRegistry.Resolution<Object, Object> resolution;
        Preconditions.checkNotNull(eventClass, (Object)"eventClass");
        Preconditions.checkNotNull((Object)identifier, (Object)"identifier");
        if (time == EventValue.Time.NOW) {
            flags = flags.without(EventValueRegistry.Flag.FALLBACK_TO_DEFAULT_TIME_STATE);
        }
        if ((resolution = this.eventValuesCache.get(input = Input.of(eventClass, identifier, time, flags))) != null) {
            return resolution;
        }
        resolution = Resolver.builder(eventClass).filter(ev -> ClassUtils.isRelatedTo(ev.eventClass(), eventClass) && ev.matchesInput(identifier)).comparator(Resolver.EVENT_DISTANCE_COMPARATOR).mapper(ev -> ev.getConverted(eventClass, ev.valueClass())).build().resolve(this.eventValues(time));
        if (resolution.successful()) {
            this.eventValuesCache.put(input, resolution);
            return resolution;
        }
        if (flags.has(EventValueRegistry.Flag.FALLBACK_TO_DEFAULT_TIME_STATE)) {
            return this.resolve(eventClass, identifier, EventValue.Time.NOW, flags);
        }
        resolution = EventValueRegistry.Resolution.empty();
        this.eventValuesCache.put(input, resolution);
        return resolution;
    }

    @Override
    public <E extends Event, V> EventValueRegistry.Resolution<E, ? extends V> resolve(Class<E> eventClass, Class<V> valueClass) {
        return this.resolve(eventClass, valueClass, EventValue.Time.NOW);
    }

    @Override
    public <E extends Event, V> EventValueRegistry.Resolution<E, ? extends V> resolve(Class<E> eventClass, Class<V> valueClass, EventValue.Time time) {
        return this.resolve(eventClass, valueClass, time, EventValueRegistry.Flags.DEFAULT);
    }

    @Override
    public <E extends Event, V> EventValueRegistry.Resolution<E, ? extends V> resolve(Class<E> eventClass, Class<V> valueClass, EventValue.Time time, EventValueRegistry.Flags flags) {
        Input<E, Class<V>> input;
        EventValueRegistry.Resolution<Object, Object> resolution;
        Preconditions.checkNotNull(eventClass, (Object)"eventClass");
        Preconditions.checkNotNull(valueClass, (Object)"valueClass");
        if (time == EventValue.Time.NOW) {
            flags = flags.without(EventValueRegistry.Flag.FALLBACK_TO_DEFAULT_TIME_STATE);
        }
        if ((resolution = this.eventValuesCache.get(input = Input.of(eventClass, valueClass, time, flags))) != null) {
            return resolution;
        }
        resolution = this.resolveExact(eventClass, valueClass, time).anyOptional().map(eventValue -> EventValueRegistry.Resolution.of(Collections.singletonList(eventValue))).orElse(EventValueRegistry.Resolution.empty());
        if (resolution.successful() || resolution.errored()) {
            this.eventValuesCache.put(input, resolution);
            return resolution;
        }
        resolution = this.resolveNearest(eventClass, valueClass, time);
        if (resolution.successful() || resolution.errored()) {
            this.eventValuesCache.put(input, resolution);
            return resolution;
        }
        if (flags.has(EventValueRegistry.Flag.ALLOW_CONVERSION)) {
            resolution = this.resolveWithDowncastConversion(eventClass, valueClass, time);
            if (resolution.successful() || resolution.errored()) {
                this.eventValuesCache.put(input, resolution);
                return resolution;
            }
            resolution = this.resolveWithConversion(eventClass, valueClass, time);
            if (resolution.successful() || resolution.errored()) {
                this.eventValuesCache.put(input, resolution);
                return resolution;
            }
        }
        if (flags.has(EventValueRegistry.Flag.FALLBACK_TO_DEFAULT_TIME_STATE)) {
            return this.resolve(eventClass, valueClass, EventValue.Time.NOW, flags);
        }
        resolution = EventValueRegistry.Resolution.empty();
        this.eventValuesCache.put(input, resolution);
        return resolution;
    }

    @Override
    public <E extends Event, V> EventValueRegistry.Resolution<E, V> resolveExact(Class<E> eventClass, Class<V> valueClass, EventValue.Time time) {
        return Resolver.builder(eventClass, valueClass).filter(ev -> ev.eventClass().isAssignableFrom(eventClass) && ev.valueClass().equals(valueClass)).comparator(Resolver.EVENT_DISTANCE_COMPARATOR).filterMatches().build().resolve(this.eventValues(time));
    }

    private <E extends Event, V> EventValueRegistry.Resolution<E, ? extends V> resolveNearest(Class<E> eventClass, Class<V> valueClass, EventValue.Time time) {
        return Resolver.builder(eventClass, valueClass).filter(ev -> ClassUtils.isRelatedTo(ev.eventClass(), eventClass) && valueClass.isAssignableFrom(ev.valueClass())).comparator(Resolver.EVENT_VALUE_DISTANCE_COMPARATOR).mapper(ev -> ev.getConverted(eventClass, valueClass)).filterMatches().build().resolve(this.eventValues(time));
    }

    private <E extends Event, V> EventValueRegistry.Resolution<E, V> resolveWithDowncastConversion(Class<E> eventClass, Class<V> valueClass, EventValue.Time time) {
        Converter<Object, Object> converter = source -> valueClass.isInstance(source) ? valueClass.cast(source) : null;
        return Resolver.builder(eventClass, valueClass).filter(ev -> ClassUtils.isRelatedTo(ev.eventClass(), eventClass) && ev.valueClass().isAssignableFrom(valueClass)).comparator(Resolver.EVENT_VALUE_DISTANCE_COMPARATOR).mapper(ev -> ev.getConverted(eventClass, valueClass, converter)).filterMatches().build().resolve(this.eventValues(time));
    }

    private <E extends Event, V> EventValueRegistry.Resolution<E, V> resolveWithConversion(Class<E> eventClass, Class<V> valueClass, EventValue.Time time) {
        return Resolver.builder(eventClass, valueClass).filter(ev -> ClassUtils.isRelatedTo(ev.eventClass(), eventClass)).comparator(Resolver.BI_EVENT_DISTANCE_COMPARATOR).mapper(ev -> ev.getConverted(eventClass, valueClass)).build().resolve(this.eventValues(time));
    }

    private List<EventValue<?, ?>> eventValues(EventValue.Time time) {
        return this.eventValues.get((Object)time);
    }

    @Override
    public @Unmodifiable List<EventValue<?, ?>> elements() {
        return this.eventValues.values().stream().flatMap(Collection::stream).toList();
    }

    @Override
    public @Unmodifiable List<EventValue<?, ?>> elements(EventValue.Time time) {
        return List.copyOf(this.eventValues(time));
    }

    @Override
    public <E extends Event> @Unmodifiable List<EventValue<? extends E, ?>> elements(Class<E> event) {
        return this.eventValues.values().stream().flatMap(Collection::stream).filter(eventValue -> event.isAssignableFrom(eventValue.eventClass())).toList();
    }

    private record Input<E extends Event, I>(Class<E> eventClass, I input, EventValue.Time time, EventValueRegistry.Flags flags) {
        static <E extends Event> Input<E, String> of(Class<E> eventClass, String input, EventValue.Time time, EventValueRegistry.Flags flags) {
            return new Input<E, String>(eventClass, input, time, flags);
        }

        static <E extends Event> Input<E, Class<?>> of(Class<E> eventClass, Class<?> input, EventValue.Time time, EventValueRegistry.Flags flags) {
            return new Input(eventClass, input, time, flags);
        }
    }

    static class UnmodifiableView
    implements EventValueRegistry {
        private final EventValueRegistry delegate;

        UnmodifiableView(EventValueRegistry delegate) {
            this.delegate = delegate;
        }

        @Override
        public <E extends Event> void register(EventValue<E, ?> eventValue) {
            throw new UnsupportedOperationException("Cannot register event values with an unmodifiable event value registry.");
        }

        @Override
        public boolean unregister(EventValue<?, ?> eventValue) {
            throw new UnsupportedOperationException("Cannot unregister event values from an unmodifiable event value registry.");
        }

        @Override
        public boolean isRegistered(EventValue<?, ?> eventValue) {
            return this.delegate.isRegistered(eventValue);
        }

        @Override
        public boolean isRegistered(Class<? extends Event> eventClass, Class<?> valueClass, EventValue.Time time) {
            return this.delegate.isRegistered(eventClass, valueClass, time);
        }

        @Override
        public <E extends Event, V> EventValueRegistry.Resolution<E, V> resolve(Class<E> eventClass, String identifier) {
            return this.delegate.resolve(eventClass, identifier);
        }

        @Override
        public <E extends Event, V> EventValueRegistry.Resolution<E, V> resolve(Class<E> eventClass, String identifier, EventValue.Time time) {
            return this.delegate.resolve(eventClass, identifier, time);
        }

        @Override
        public <E extends Event, V> EventValueRegistry.Resolution<E, V> resolve(Class<E> eventClass, String identifier, EventValue.Time time, EventValueRegistry.Flags flags) {
            return this.delegate.resolve(eventClass, identifier, time, flags);
        }

        @Override
        public <E extends Event, V> EventValueRegistry.Resolution<E, ? extends V> resolve(Class<E> eventClass, Class<V> valueClass) {
            return this.delegate.resolve(eventClass, valueClass);
        }

        @Override
        public <E extends Event, V> EventValueRegistry.Resolution<E, ? extends V> resolve(Class<E> eventClass, Class<V> valueClass, EventValue.Time time) {
            return this.delegate.resolve(eventClass, valueClass, time);
        }

        @Override
        public <E extends Event, V> EventValueRegistry.Resolution<E, ? extends V> resolve(Class<E> eventClass, Class<V> valueClass, EventValue.Time time, EventValueRegistry.Flags flags) {
            return this.delegate.resolve(eventClass, valueClass, time, flags);
        }

        @Override
        public <E extends Event, V> EventValueRegistry.Resolution<E, V> resolveExact(Class<E> eventClass, Class<V> valueClass, EventValue.Time time) {
            return this.delegate.resolveExact(eventClass, valueClass, time);
        }

        @Override
        public @Unmodifiable List<EventValue<?, ?>> elements() {
            return this.delegate.elements();
        }

        @Override
        public @Unmodifiable List<EventValue<?, ?>> elements(EventValue.Time time) {
            return this.delegate.elements(time);
        }

        @Override
        public <E extends Event> @Unmodifiable List<EventValue<? extends E, ?>> elements(Class<E> event) {
            return this.delegate.elements(event);
        }

        @Override
        public EventValueRegistry unmodifiableView() {
            return this;
        }
    }
}

