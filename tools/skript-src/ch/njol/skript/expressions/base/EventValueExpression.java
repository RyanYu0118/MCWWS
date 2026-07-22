/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions.base;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import com.google.common.base.Preconditions;
import java.lang.invoke.TypeDescriptor;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.util.Priority;

public class EventValueExpression<T>
extends SimpleExpression<T>
implements DefaultExpression<T> {
    public static final Priority DEFAULT_PRIORITY = Priority.before(SyntaxInfo.COMBINED);
    private static final EventValueRegistry.Flags NO_CONVERSION_FLAGS = EventValueRegistry.Flags.DEFAULT.without(EventValueRegistry.Flag.ALLOW_CONVERSION);
    private final EventValueRegistry registry = Skript.instance().registry(EventValueRegistry.class);
    public final Set<Class<? extends Event>> events = new HashSet<Class<? extends Event>>();
    @Nullable
    private final Class<?> componentType;
    @Nullable
    private final Class<? extends T> type;
    @Nullable
    private final String identifier;
    @Nullable
    private Changer<? super T> changer;
    private final Kleenean single;
    private final boolean exact;
    private boolean isDelayed;

    public static <E extends EventValueExpression<T>, T> DefaultSyntaxInfos.Expression.Builder<? extends DefaultSyntaxInfos.Expression.Builder<?, E, T>, E, T> infoBuilder(Class<E> expressionClass, Class<T> returnType, String ... patterns) {
        for (int i = 0; i < patterns.length; ++i) {
            patterns[i] = "[the] " + patterns[i];
        }
        return (DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(expressionClass, returnType).priority(DEFAULT_PRIORITY)).addPatterns(patterns);
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <T> void register(Class<? extends EventValueExpression<T>> expression, Class<T> type, String pattern) {
        Skript.registerExpression(expression, type, ExpressionType.EVENT, "[the] " + pattern);
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static <T> void register(Class<? extends EventValueExpression<T>> expression, Class<T> type, String ... patterns) {
        for (int i = 0; i < patterns.length; ++i) {
            if (StringUtils.startsWithIgnoreCase(patterns[i], "[the] ")) continue;
            patterns[i] = "[the] " + patterns[i];
        }
        Skript.registerExpression(expression, type, ExpressionType.EVENT, patterns);
    }

    public EventValueExpression(Class<? extends T> type) {
        this(type, null);
    }

    public EventValueExpression(Class<? extends T> type, boolean exact) {
        this(type, null, exact);
    }

    public EventValueExpression(Class<? extends T> type, @Nullable Changer<? super T> changer) {
        this(type, changer, false);
    }

    public EventValueExpression(Class<? extends T> type, @Nullable Changer<? super T> changer, boolean exact) {
        this((Class)Preconditions.checkNotNull(type, (Object)"type"), null, changer, exact);
    }

    public EventValueExpression(String identifier) {
        this(identifier, null);
    }

    public EventValueExpression(String identifier, @Nullable Changer<? super T> changer) {
        this(null, (String)Preconditions.checkNotNull((Object)identifier, (Object)"identifier"), changer, false);
    }

    @Contract(value="null, null, _, _ -> fail")
    public EventValueExpression(@Nullable Class<? extends T> type, @Nullable String identifier, @Nullable Changer<? super T> changer, boolean exact) {
        if (type == null && identifier == null) {
            throw new IllegalArgumentException("Either type or identifier must be non-null");
        }
        this.type = type;
        this.identifier = identifier;
        this.exact = exact;
        this.changer = changer;
        this.single = type != null ? Kleenean.get(!type.isArray()) : Kleenean.UNKNOWN;
        this.componentType = this.single.isTrue() || this.single.isUnknown() ? type : type.getComponentType();
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        if (expressions.length != 0) {
            throw new SkriptAPIException(this.getClass().getName() + " has expressions in its pattern but does not override init(...)");
        }
        return this.init();
    }

    private <E extends Event> EventValueRegistry.Resolution<E, ? extends T> resolve(Class<E> eventClass) {
        return this.resolve(eventClass, EventValueRegistry.Flags.DEFAULT);
    }

    private <E extends Event> EventValueRegistry.Resolution<E, ? extends T> resolve(Class<E> eventClass, EventValueRegistry.Flags flags) {
        return this.resolve(eventClass, EventValue.Time.of(this.getTime()), flags);
    }

    private <E extends Event> EventValueRegistry.Resolution<E, ? extends T> resolveForTime(Class<E> eventClass, EventValue.Time time) {
        return this.resolve(eventClass, time, EventValueRegistry.Flags.DEFAULT.without(EventValueRegistry.Flag.FALLBACK_TO_DEFAULT_TIME_STATE));
    }

    private <E extends Event> EventValueRegistry.Resolution<E, ? extends T> resolve(Class<E> eventClass, EventValue.Time time, EventValueRegistry.Flags flags) {
        if (this.identifier != null) {
            EventValueRegistry.Resolution resolution = this.registry.resolve(eventClass, this.identifier, time, flags);
            if (this.type == null) {
                return resolution;
            }
            return EventValueRegistry.Resolution.of(resolution.all().stream().map(eventValue -> eventValue.getConverted(eventClass, this.type)).filter(Objects::nonNull).toList());
        }
        return this.exact ? this.registry.resolveExact(eventClass, this.type, time) : this.registry.resolve(eventClass, this.type, time, flags);
    }

    private String input(boolean plural) {
        if (this.identifier != null) {
            return this.identifier;
        }
        assert (this.componentType != null);
        return Classes.getSuperClassInfo(this.componentType).getName().toString(plural);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean init() {
        ParserInstance parser = this.getParser();
        this.isDelayed = parser.getHasDelayBefore().isTrue();
        ParseLogHandler log = SkriptLogger.startParseLogHandler();
        try {
            boolean hasValue = false;
            Class<? extends Event>[] events = parser.getCurrentEvents();
            if (events == null) {
                assert (false);
                boolean bl = false;
                return bl;
            }
            for (Class<? extends Event> event : events) {
                EventValueRegistry.Resolution<Event, T> resolution = this.resolve(event, NO_CONVERSION_FLAGS);
                if (resolution.multiple()) {
                    log.printError("There are multiple " + this.input(true) + " in " + Utils.a(parser.getCurrentEventName()) + " event. You must define which " + this.input(false) + " to use.");
                    boolean bl = false;
                    return bl;
                }
                resolution = this.resolve(event);
                if (!resolution.successful()) continue;
                hasValue = true;
            }
            if (!hasValue) {
                String message = null;
                if (this.type != null) {
                    TypeDescriptor.OfField<Class<?>> suggested;
                    TypeDescriptor.OfField<Class<?>> ofField = suggested = this.type.isArray() ? this.componentType : this.type.arrayType();
                    assert (suggested != null);
                    EventValueExpression suggestedEventValue = new EventValueExpression((Class<?>)suggested);
                    boolean suggestedValueExists = false;
                    for (Class<? extends Event> event : events) {
                        if (suggestedEventValue.resolve(event, NO_CONVERSION_FLAGS).multiple() || !suggestedEventValue.resolve(event).successful()) continue;
                        suggestedValueExists = true;
                        break;
                    }
                    if (suggestedValueExists) {
                        message = ((Class)suggested).isArray() ? "There are multiple " + suggestedEventValue.input(true) : "There's only one " + suggestedEventValue.input(false);
                        message = (String)message + " in " + Utils.a(parser.getCurrentEventName()) + " event. Did you mean 'event-" + suggestedEventValue.input(((Class)suggested).isArray()) + "'?";
                    }
                }
                if (message == null) {
                    boolean single = this.isSingle();
                    String is = single ? "'s" : " are";
                    message = "There" + is + " no " + this.input(!single) + " in " + Utils.a(parser.getCurrentEventName()) + " event.";
                }
                log.printError(message);
                int n = 0;
                return n != 0;
            }
            log.printLog();
            this.events.addAll(Arrays.asList(events));
            boolean bl = true;
            return bl;
        }
        finally {
            log.stop();
        }
    }

    @Override
    protected T @Nullable [] get(Event event) {
        T value = this.getValue(event);
        if (value == null) {
            return (Object[])Array.newInstance(this.getReturnType(), 0);
        }
        if (this.isSingle()) {
            Object[] one = (Object[])Array.newInstance(this.getReturnType(), 1);
            one[0] = value;
            return one;
        }
        Object[] dataArray = (Object[])value;
        Object[] array = (Object[])Array.newInstance(this.getReturnType(), dataArray.length);
        System.arraycopy(dataArray, 0, array, 0, array.length);
        return array;
    }

    @Nullable
    private <E extends Event> T getValue(E event) {
        Class<E> eventClass = this.getParseTimeEventClass(event);
        if (eventClass == null) {
            return null;
        }
        EventValueRegistry.Resolution<E, T> resolution = this.resolve(eventClass);
        return resolution.anyOptional().map(eventValue -> eventValue.get(event)).orElse(null);
    }

    private <E extends Event> Class<E> getParseTimeEventClass(E event) {
        for (Class<? extends Event> eventClass : this.events) {
            if (!eventClass.isInstance(event)) continue;
            return eventClass;
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        for (Class<? extends Event> event : this.events) {
            EventValue found;
            EventValueRegistry.Resolution<Event, T> resolution = this.resolve(event);
            if (!resolution.successful() || (found = (EventValue)resolution.all().stream().filter(eventValue -> eventValue.hasChanger(mode)).findFirst().orElse(null)) == null) continue;
            if (this.isDelayed) {
                Skript.error("Event values cannot be changed after the event has already passed.");
                return null;
            }
            return CollectionUtils.array(found.valueClass());
        }
        if (this.changer == null) {
            this.changer = Classes.getSuperClassInfo(this.getReturnType()).getChanger();
        }
        return this.changer == null ? null : this.changer.acceptChange(mode);
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Class<Event> eventClass = this.getParseTimeEventClass(event);
        if (eventClass == null) {
            return;
        }
        EventValueRegistry.Resolution<Event, T> resolution = this.resolve(eventClass);
        for (EventValue eventValue : resolution.all()) {
            if (!eventValue.hasChanger(mode)) continue;
            eventValue.changer(mode).ifPresent(changer -> {
                if (!eventValue.valueClass().isArray() && delta != null) {
                    changer.change(event, delta[0]);
                } else {
                    changer.change(event, delta);
                }
            });
            return;
        }
        if (this.changer != null) {
            Changer.ChangerUtils.change(this.changer, this.getArray(event), delta, mode);
        }
    }

    @Override
    public boolean setTime(int time) {
        Class<? extends Event>[] events = this.getParser().getCurrentEvents();
        if (events == null) {
            assert (false);
            return false;
        }
        for (Class<? extends Event> event : events) {
            assert (event != null);
            if (!this.resolveForTime(event, EventValue.Time.PAST).successful() && !this.resolveForTime(event, EventValue.Time.FUTURE).successful()) continue;
            super.setTime(time);
            this.events.clear();
            this.init();
            return true;
        }
        return false;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public boolean isSingle() {
        if (!this.single.isUnknown()) {
            return this.single.isTrue();
        }
        for (Class<? extends Event> event : this.events) {
            Class<T> valueClass;
            EventValueRegistry.Resolution<Event, T> resolution = this.resolve(event);
            if (!resolution.successful() || !(valueClass = resolution.any().valueClass()).isArray()) continue;
            return false;
        }
        return true;
    }

    @Override
    public Class<? extends T>[] possibleReturnTypes() {
        if (this.componentType != null) {
            return new Class[]{this.componentType};
        }
        HashSet types = new HashSet();
        for (Class<? extends Event> eventClass : this.events) {
            EventValueRegistry.Resolution<Event, T> resolution = this.resolve(eventClass);
            if (!resolution.successful()) continue;
            resolution.anyOptional().ifPresent(eventValue -> {
                Class type = eventValue.valueClass();
                type = type.isArray() ? type.componentType() : type;
                types.add(type);
            });
        }
        return types.toArray(new Class[0]);
    }

    @Override
    public Class<? extends T> getReturnType() {
        Class<T>[] classes = this.possibleReturnTypes();
        if (classes.length == 1) {
            return classes[0];
        }
        return Utils.highestDenominator(Object.class, classes);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (!debug || event == null) {
            return "event-" + this.input(!this.isSingle());
        }
        return Classes.getDebugMessage(this.getValue(event));
    }
}

