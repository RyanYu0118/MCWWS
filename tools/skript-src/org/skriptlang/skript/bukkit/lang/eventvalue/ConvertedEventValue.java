/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.bukkit.lang.eventvalue;

import ch.njol.skript.classes.Changer;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Optional;
import java.util.SequencedCollection;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.Converters;

record ConvertedEventValue<SourceEvent extends Event, ConvertedEvent extends Event, SourceValue, ConvertedValue>(Class<ConvertedEvent> eventClass, Class<ConvertedValue> valueClass, EventValue<SourceEvent, SourceValue> source, Converter<SourceValue, ConvertedValue> valueConverter, @Nullable Converter<ConvertedValue, SourceValue> reverseConverter) implements EventValue<ConvertedEvent, ConvertedValue>
{
    public ConvertedEventValue(Class<ConvertedEvent> eventClass, Class<ConvertedValue> valueClass, EventValue<SourceEvent, SourceValue> source, Converter<SourceValue, ConvertedValue> valueConverter, @Nullable Converter<ConvertedValue, SourceValue> reverseConverter) {
        this.eventClass = eventClass;
        this.valueClass = valueClass;
        this.source = source;
        this.valueConverter = valueConverter;
        this.reverseConverter = reverseConverter == null ? ConvertedEventValue.getConverter(valueClass, source.valueClass()) : reverseConverter;
    }

    public static <SourceEvent extends Event, ConvertedEvent extends Event, SourceValue, ConvertedValue> EventValue<ConvertedEvent, ConvertedValue> newInstance(Class<ConvertedEvent> eventClass, Class<ConvertedValue> valueClass, EventValue<SourceEvent, SourceValue> source) {
        if (source.eventClass().isAssignableFrom(eventClass) && valueClass.isAssignableFrom(source.valueClass())) {
            return source;
        }
        Converter<SourceValue, ConvertedValue> converter = ConvertedEventValue.getConverter(source.valueClass(), valueClass);
        if (converter == null) {
            return null;
        }
        return new ConvertedEventValue<SourceEvent, ConvertedEvent, SourceValue, ConvertedValue>(eventClass, valueClass, source, converter, ConvertedEventValue.getConverter(valueClass, source.valueClass()));
    }

    @Nullable
    private static <F, T> Converter<F, T> getConverter(Class<F> from, Class<T> to) {
        if (from.isArray() && to.isArray()) {
            Converter componentConverter = ConvertedEventValue.getConverter(from.componentType(), to.componentType());
            if (componentConverter == null) {
                return null;
            }
            return obj -> {
                Object converted = to.cast(Array.newInstance(to.componentType(), Array.getLength(obj)));
                int length = Array.getLength(converted);
                for (int i = 0; i < length; ++i) {
                    Object convertedObj = componentConverter.convert(Array.get(obj, i));
                    if (convertedObj == null) {
                        return null;
                    }
                    Array.set(converted, i, convertedObj);
                }
                return converted;
            };
        }
        return to.isAssignableFrom(from) ? value -> value : Converters.getConverter(from, to);
    }

    @Override
    public @Unmodifiable SequencedCollection<String> patterns() {
        return this.source.patterns();
    }

    @Override
    public EventValue.Validation validate(Class<?> event) {
        return this.source.validate(event);
    }

    @Override
    public boolean matchesInput(String input) {
        return this.source.matchesInput(input);
    }

    @Override
    public ConvertedValue get(ConvertedEvent event) {
        return this.converter().convert(event);
    }

    @Override
    public Converter<ConvertedEvent, ConvertedValue> converter() {
        return event -> {
            if (!this.source.eventClass().isAssignableFrom(event.getClass())) {
                return null;
            }
            SourceValue sourceValue = this.source.get((Event)this.source.eventClass().cast(event));
            return this.valueConverter.convert(sourceValue);
        };
    }

    @Override
    public boolean hasChanger(Changer.ChangeMode mode) {
        return this.source.hasChanger(mode);
    }

    @Override
    public Optional<EventValue.Changer<ConvertedEvent, ConvertedValue>> changer(Changer.ChangeMode mode) {
        return this.source.changer(mode).map(changer -> (event, value) -> {
            if (!this.source.eventClass().isAssignableFrom(event.getClass())) {
                return;
            }
            if (this.reverseConverter == null) {
                return;
            }
            SourceValue sourceValue = this.reverseConverter.convert(value);
            if (sourceValue != null) {
                changer.change((Event)this.source.eventClass().cast(event), sourceValue);
            }
        });
    }

    @Override
    public EventValue.Time time() {
        return this.source.time();
    }

    @Override
    public @Unmodifiable Collection<Class<? extends ConvertedEvent>> excludedEvents() {
        return this.source.excludedEvents().stream().filter(this.eventClass::isAssignableFrom).toList();
    }

    @Override
    @Nullable
    public String excludedErrorMessage() {
        return this.source.excludedErrorMessage();
    }

    @Override
    public boolean matches(EventValue<?, ?> eventValue) {
        return this.matches(eventValue.eventClass(), eventValue.valueClass(), eventValue.patterns());
    }

    @Override
    @Nullable
    public <NewEvent extends Event, NewValue> EventValue<NewEvent, NewValue> getConverted(Class<NewEvent> newEventClass, Class<NewValue> newValueClass) {
        return ConvertedEventValue.newInstance(newEventClass, newValueClass, this.source);
    }

    @Override
    public <NewEvent extends Event, NewValue> EventValue<NewEvent, NewValue> getConverted(Class<NewEvent> newEventClass, Class<NewValue> newValueClass, Converter<ConvertedValue, NewValue> converter, @Nullable Converter<NewValue, ConvertedValue> reverseConverter) {
        return new ConvertedEventValue<SourceEvent, NewEvent, ConvertedValue, NewValue>(newEventClass, newValueClass, this, converter, reverseConverter);
    }
}

