/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Multimap
 *  com.google.common.collect.MultimapBuilder
 *  com.google.common.collect.SetMultimap
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package ch.njol.skript.registrations;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.registrations.EventConverter;
import ch.njol.util.Kleenean;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.lang.converter.Converter;

@Deprecated(since="2.15", forRemoval=true)
public class EventValues {
    public static final int TIME_PAST = EventValue.Time.PAST.value();
    public static final int TIME_NOW = EventValue.Time.NOW.value();
    public static final int TIME_FUTURE = EventValue.Time.FUTURE.value();
    private static EventValueRegistry registry;

    private EventValues() {
    }

    @ApiStatus.Internal
    public static void setEventValueRegistry(EventValueRegistry registry) {
        if (EventValues.registry != null) {
            throw new IllegalStateException("EventValueRegistry is already set and cannot be changed.");
        }
        EventValues.registry = registry;
    }

    public static @Unmodifiable List<EventValueInfo<?, ?>> getEventValuesListForTime(int time) {
        return registry.elements(EventValue.Time.of(time)).stream().map(EventValueInfo::fromModern).toList();
    }

    public static <T, E extends Event> void registerEventValue(Class<E> eventClass, Class<T> valueClass, Converter<E, T> converter) {
        EventValues.registerEventValue(eventClass, valueClass, converter, TIME_NOW);
    }

    public static <T, E extends Event> void registerEventValue(Class<E> eventClass, Class<T> valueClass, Converter<E, T> converter, int time) {
        EventValues.registerEventValue(eventClass, valueClass, converter, time, null, null);
    }

    @SafeVarargs
    public static <T, E extends Event> void registerEventValue(Class<E> eventClass, Class<T> valueClass, Converter<E, T> converter, int time, @Nullable String excludeErrorMessage, Class<? extends E> ... excludes) {
        EventValue.Builder<Event, T> builder = EventValue.builder(eventClass, valueClass).getter(converter).time(EventValue.Time.of(time)).excludedErrorMessage(excludeErrorMessage).excludes(excludes);
        if (converter instanceof EventConverter) {
            EventConverter eventConverter = (EventConverter)converter;
            builder.registerChanger(Changer.ChangeMode.SET, eventConverter::set);
        }
        registry.register(builder.build());
    }

    @Nullable
    public static <T, E extends Event> T getEventValue(E event, Class<T> valueClass, int time) {
        return registry.resolve(event.getClass(), valueClass, EventValue.Time.of(time)).uniqueOptional().map(eventValue -> eventValue.get(event)).orElse(null);
    }

    @Nullable
    public static <E extends Event, T> Converter<? super E, ? extends T> getExactEventValueConverter(Class<E> eventClass, Class<T> valueClass, int time) {
        return registry.resolveExact(eventClass, valueClass, EventValue.Time.of(time)).anyOptional().map(EventValue::converter).orElse(null);
    }

    public static <T, E extends Event> Kleenean hasMultipleConverters(Class<E> eventClass, Class<T> valueClass, int time) {
        List<Converter<E, T>> getters = EventValues.getEventValueConverters(eventClass, valueClass, time, true, false);
        if (getters == null) {
            return Kleenean.UNKNOWN;
        }
        return Kleenean.get(getters.size() > 1);
    }

    @Nullable
    public static <T, E extends Event> Converter<? super E, ? extends T> getEventValueConverter(Class<E> eventClass, Class<T> valueClass, int time) {
        return EventValues.getEventValueConverter(eventClass, valueClass, time, true);
    }

    @Nullable
    private static <T, E extends Event> Converter<? super E, ? extends T> getEventValueConverter(Class<E> eventClass, Class<T> valueClass, int time, boolean allowDefault) {
        List<Converter<E, T>> list = EventValues.getEventValueConverters(eventClass, valueClass, time, allowDefault);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Nullable
    private static <T, E extends Event> List<Converter<? super E, ? extends T>> getEventValueConverters(Class<E> eventClass, Class<T> valueClass, int time, boolean allowDefault) {
        return EventValues.getEventValueConverters(eventClass, valueClass, time, allowDefault, true);
    }

    @Nullable
    private static <T, E extends Event> List<Converter<? super E, ? extends T>> getEventValueConverters(Class<E> eventClass, Class<T> valueClass, int time, boolean allowDefault, boolean allowConverting) {
        EventValueRegistry.Resolution<E, T> resolution;
        EventValueRegistry.Flags flags = EventValueRegistry.Flags.NONE;
        if (allowDefault) {
            flags = flags.with(EventValueRegistry.Flag.FALLBACK_TO_DEFAULT_TIME_STATE);
        }
        if (allowConverting) {
            flags = flags.with(EventValueRegistry.Flag.ALLOW_CONVERSION);
        }
        if (!(resolution = registry.resolve(eventClass, valueClass, EventValue.Time.of(time), flags)).successful()) {
            return null;
        }
        return resolution.all().stream().map(EventValue::converter).toList();
    }

    public static boolean doesExactEventValueHaveTimeStates(Class<? extends Event> eventClass, Class<?> valueClass) {
        return EventValues.getExactEventValueConverter(eventClass, valueClass, TIME_PAST) != null || EventValues.getExactEventValueConverter(eventClass, valueClass, TIME_FUTURE) != null;
    }

    public static boolean doesEventValueHaveTimeStates(Class<? extends Event> eventClass, Class<?> valueClass) {
        return EventValues.getEventValueConverter(eventClass, valueClass, TIME_PAST, false) != null || EventValues.getEventValueConverter(eventClass, valueClass, TIME_FUTURE, false) != null;
    }

    public static int[] getTimeStates() {
        return new int[]{TIME_PAST, TIME_NOW, TIME_FUTURE};
    }

    public static Multimap<Class<? extends Event>, EventValueInfo<?, ?>> getPerEventEventValues() {
        SetMultimap eventValues = MultimapBuilder.hashKeys().hashSetValues().build();
        for (int time : EventValues.getTimeStates()) {
            for (EventValueInfo<?, ?> eventValueInfo : EventValues.getEventValuesListForTime(time)) {
                Collection existing = eventValues.get(eventValueInfo.eventClass);
                existing.add(eventValueInfo);
                eventValues.putAll(eventValueInfo.eventClass, (Iterable)existing);
            }
        }
        return eventValues;
    }

    public record EventValueInfo<E extends Event, T>(Class<E> eventClass, Class<T> valueClass, Converter<E, T> converter, @Nullable String excludeErrorMessage, @Nullable Class<? extends E>[] excludes, int time) {
        public EventValueInfo(Class<E> eventClass, Class<T> valueClass, Converter<E, T> converter, @Nullable String excludeErrorMessage, @Nullable Class<? extends E>[] excludes, int time) {
            assert (eventClass != null);
            assert (valueClass != null);
            assert (converter != null);
        }

        public static <E extends Event, T> EventValueInfo<E, T> fromModern(final EventValue<E, T> eventValue) {
            return new EventValueInfo<E, T>(eventValue.eventClass(), eventValue.valueClass(), eventValue.changer(Changer.ChangeMode.SET).map(changer -> new EventConverter<E, T>(){
                final /* synthetic */ EventValue.Changer val$changer;
                {
                    this.val$changer = changer;
                }

                @Override
                @Nullable
                public T convert(E from) {
                    return eventValue.get(from);
                }

                @Override
                public void set(E event, @Nullable T value) {
                    this.val$changer.change(event, value);
                }
            }).orElse(eventValue.converter()), eventValue.excludedErrorMessage(), eventValue.excludedEvents().toArray(new Class[0]), eventValue.time().value());
        }
    }
}

