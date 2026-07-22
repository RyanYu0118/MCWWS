/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.bukkit.lang.eventvalue;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.patterns.MatchResult;
import ch.njol.skript.patterns.PatternCompiler;
import ch.njol.skript.patterns.PatternElement;
import ch.njol.skript.patterns.RegexPatternElement;
import ch.njol.skript.patterns.SkriptPattern;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import com.google.common.base.MoreObjects;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.function.BiPredicate;
import java.util.function.Function;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.bukkit.lang.eventvalue.ConvertedEventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.lang.converter.Converter;

final class EventValueImpl<E extends Event, V>
implements EventValue<E, V> {
    private final Class<E> eventClass;
    private final Class<V> valueClass;
    private final SequencedCollection<String> patterns;
    private final boolean hasCustomInputValidator;
    @Nullable
    private final BiPredicate<String, SkriptParser.ParseResult> inputValidator;
    @Nullable
    private final Function<Class<?>, EventValue.Validation> eventValidator;
    private final Converter<E, V> converter;
    private final Map<Changer.ChangeMode, EventValue.Changer<E, V>> changers;
    private final EventValue.Time time;
    private final Collection<Class<? extends E>> excludedEvents;
    @Nullable
    private final String excludedErrorMessage;
    private SkriptPattern[] compiledPatterns;

    private EventValueImpl(BuilderImpl<E, V> builder) {
        this.eventClass = builder.eventClass;
        this.valueClass = builder.valueClass;
        this.patterns = builder.patterns;
        this.hasCustomInputValidator = builder.hasCustomInputValidator;
        this.inputValidator = builder.inputValidator;
        this.eventValidator = builder.eventValidator;
        this.converter = builder.converter;
        this.changers = builder.changers;
        this.time = builder.time;
        this.excludedEvents = builder.excludedEvents;
        this.excludedErrorMessage = builder.excludedErrorMessage;
    }

    @Override
    public Class<E> eventClass() {
        return this.eventClass;
    }

    @Override
    public Class<V> valueClass() {
        return this.valueClass;
    }

    @Override
    public @Unmodifiable SequencedCollection<String> patterns() {
        return this.patterns;
    }

    @Override
    public EventValue.Validation validate(Class<?> event) {
        for (Class<E> excludedEvent : this.excludedEvents) {
            if (!excludedEvent.isAssignableFrom(event)) continue;
            if (this.excludedErrorMessage != null) {
                Skript.error(this.excludedErrorMessage);
            }
            return EventValue.Validation.ABORT;
        }
        if (this.eventValidator == null) {
            return EventValue.Validation.VALID;
        }
        return this.eventValidator.apply(event);
    }

    @Override
    public boolean matchesInput(String input) {
        for (SkriptPattern pattern : this.compilePatterns()) {
            MatchResult match = pattern.match(input);
            if (match == null || this.inputValidator != null && !this.inputValidator.test(input, match.toParseResult())) continue;
            return true;
        }
        return false;
    }

    private SkriptPattern[] compilePatterns() {
        if (this.compiledPatterns != null) {
            return this.compiledPatterns;
        }
        this.compiledPatterns = this.patterns.isEmpty() ? this.patternsFromType(this.valueClass) : (SkriptPattern[])this.patterns.stream().map(PatternCompiler::compile).toArray(SkriptPattern[]::new);
        return this.compiledPatterns;
    }

    private SkriptPattern[] patternsFromType(Class<?> type) {
        ClassInfo<?> info;
        boolean plural = ((Class)type).isArray();
        if (plural) {
            type = ((Class)type).componentType();
        }
        if ((info = Classes.getExactClassInfo(type)) == null || info.getUserInputPatterns() == null) {
            String name = ((Class)type).getSimpleName().toLowerCase(Locale.ENGLISH);
            return new SkriptPattern[]{PatternCompiler.compile(Utils.toEnglishPlural(name, plural))};
        }
        return (SkriptPattern[])Arrays.stream(info.getUserInputPatterns()).map(RegexPatternElement::new).map(pattern -> new SkriptPattern((PatternElement)pattern, 0)).toArray(SkriptPattern[]::new);
    }

    @Override
    public V get(E event) {
        return this.converter.convert(event);
    }

    @Override
    public Converter<E, V> converter() {
        return this.converter;
    }

    @Override
    public boolean hasChanger(Changer.ChangeMode mode) {
        return this.changers.containsKey((Object)mode);
    }

    @Override
    public Optional<EventValue.Changer<E, V>> changer(Changer.ChangeMode mode) {
        return Optional.ofNullable(this.changers.get((Object)mode));
    }

    @Override
    public EventValue.Time time() {
        return this.time;
    }

    @Override
    public @Unmodifiable Collection<Class<? extends E>> excludedEvents() {
        return this.excludedEvents;
    }

    @Override
    @Nullable
    public String excludedErrorMessage() {
        return this.excludedErrorMessage;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean matches(EventValue<?, ?> eventValue) {
        if (!this.matches(eventValue.eventClass(), eventValue.valueClass(), eventValue.patterns())) return false;
        if (!(eventValue instanceof EventValueImpl)) return false;
        EventValueImpl other = (EventValueImpl)eventValue;
        if (this.hasCustomInputValidator != other.hasCustomInputValidator) return false;
        if (this.hasCustomInputValidator) {
            if (this.inputValidator != other.inputValidator) return false;
        }
        if (this.eventValidator != other.eventValidator) return false;
        if (!this.excludedEvents.equals(other.excludedEvents)) return false;
        return true;
    }

    @Override
    @Nullable
    public <ConvertedEvent extends Event, ConvertedValue> EventValue<ConvertedEvent, ConvertedValue> getConverted(Class<ConvertedEvent> newEventClass, Class<ConvertedValue> newValueClass) {
        return ConvertedEventValue.newInstance(newEventClass, newValueClass, this);
    }

    @Override
    public <ConvertedEvent extends Event, ConvertedValue> EventValue<ConvertedEvent, ConvertedValue> getConverted(Class<ConvertedEvent> newEventClass, Class<ConvertedValue> newValueClass, Converter<V, ConvertedValue> converter, @Nullable Converter<ConvertedValue, V> reverseConverter) {
        return new ConvertedEventValue(newEventClass, newValueClass, this, converter, reverseConverter);
    }

    public String toString() {
        return MoreObjects.toStringHelper((Object)this).add("eventClass", this.eventClass).add("valueClass", this.valueClass).add("patterns", this.patterns).add("time", (Object)this.time).toString();
    }

    static class BuilderImpl<E extends Event, V>
    implements EventValue.Builder<E, V> {
        private final Class<E> eventClass;
        private final Class<V> valueClass;
        private final Map<Changer.ChangeMode, EventValue.Changer<E, V>> changers = new EnumMap<Changer.ChangeMode, EventValue.Changer<E, V>>(Changer.ChangeMode.class);
        private SequencedCollection<String> patterns = Collections.emptyList();
        private boolean hasCustomInputValidator;
        @Nullable
        private BiPredicate<String, SkriptParser.ParseResult> inputValidator;
        @Nullable
        private Function<Class<?>, EventValue.Validation> eventValidator;
        private Converter<E, V> converter;
        private EventValue.Time time = EventValue.Time.NOW;
        private Collection<Class<? extends E>> excludedEvents = Collections.emptyList();
        @Nullable
        private String excludedErrorMessage;

        BuilderImpl(Class<E> eventClass, Class<V> valueClass) {
            this.eventClass = eventClass;
            this.valueClass = valueClass;
        }

        @Override
        public EventValue.Builder<E, V> patterns(String ... patterns) {
            this.patterns = patterns != null ? List.of(patterns) : Collections.emptyList();
            return this;
        }

        @Override
        public EventValue.Builder<E, V> inputValidator(BiPredicate<String, SkriptParser.ParseResult> inputValidator) {
            this.inputValidator = inputValidator;
            this.hasCustomInputValidator = inputValidator != null;
            return this;
        }

        @Override
        public EventValue.Builder<E, V> eventValidator(Function<Class<?>, EventValue.Validation> eventValidator) {
            this.eventValidator = eventValidator;
            return this;
        }

        @Override
        public EventValue.Builder<E, V> getter(Converter<E, V> converter) {
            this.converter = converter;
            return this;
        }

        @Override
        public EventValue.Builder<E, V> registerChanger(Changer.ChangeMode mode, EventValue.Changer<E, V> changer) {
            this.changers.put(mode, changer);
            return this;
        }

        @Override
        public EventValue.Builder<E, V> time(EventValue.Time time) {
            this.time = time;
            return this;
        }

        @Override
        @SafeVarargs
        public final EventValue.Builder<E, V> excludes(Class<? extends E> ... events) {
            this.excludedEvents = events != null ? List.of(events) : Collections.emptyList();
            return this;
        }

        @Override
        public EventValue.Builder<E, V> excludedErrorMessage(String excludedErrorMessage) {
            this.excludedErrorMessage = excludedErrorMessage;
            return this;
        }

        @Override
        public EventValue<E, V> build() {
            boolean plural;
            ClassInfo<V> type;
            if (this.patterns == null && (type = Classes.getExactClassInfo((plural = this.valueClass.isArray()) ? this.valueClass.getComponentType() : this.valueClass)) != null && type.getUserInputPatterns() != null) {
                this.inputValidator = BuilderImpl.combinePredicates((input, parseResult) -> plural == Utils.getEnglishPlural(input).getSecond(), this.inputValidator);
            }
            return new EventValueImpl(this);
        }

        private static <T, U> BiPredicate<T, U> combinePredicates(@Nullable BiPredicate<T, U> first, @Nullable BiPredicate<T, U> second) {
            if (first == null) {
                return second;
            }
            if (second == null) {
                return first;
            }
            return first.and(second);
        }
    }
}

