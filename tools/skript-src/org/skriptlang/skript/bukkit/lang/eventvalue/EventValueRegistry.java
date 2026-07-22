/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.bukkit.lang.eventvalue;

import ch.njol.skript.Skript;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistryImpl;
import org.skriptlang.skript.util.Registry;
import org.skriptlang.skript.util.ViewProvider;

public interface EventValueRegistry
extends Registry<EventValue<?, ?>>,
ViewProvider<EventValueRegistry> {
    public static EventValueRegistry empty(Skript skript) {
        return new EventValueRegistryImpl(skript);
    }

    public <E extends Event> void register(EventValue<E, ?> var1);

    public boolean unregister(EventValue<?, ?> var1);

    public boolean isRegistered(EventValue<?, ?> var1);

    public boolean isRegistered(Class<? extends Event> var1, Class<?> var2, EventValue.Time var3);

    public <E extends Event, V> Resolution<E, V> resolve(Class<E> var1, String var2);

    public <E extends Event, V> Resolution<E, V> resolve(Class<E> var1, String var2, EventValue.Time var3);

    public <E extends Event, V> Resolution<E, V> resolve(Class<E> var1, String var2, EventValue.Time var3, Flags var4);

    public <E extends Event, V> Resolution<E, ? extends V> resolve(Class<E> var1, Class<V> var2);

    public <E extends Event, V> Resolution<E, ? extends V> resolve(Class<E> var1, Class<V> var2, EventValue.Time var3);

    public <E extends Event, V> Resolution<E, ? extends V> resolve(Class<E> var1, Class<V> var2, EventValue.Time var3, Flags var4);

    public <E extends Event, V> Resolution<E, V> resolveExact(Class<E> var1, Class<V> var2, EventValue.Time var3);

    @Override
    public @Unmodifiable List<EventValue<?, ?>> elements();

    public @Unmodifiable List<EventValue<?, ?>> elements(EventValue.Time var1);

    public <E extends Event> @Unmodifiable List<EventValue<? extends E, ?>> elements(Class<E> var1);

    @Override
    default public EventValueRegistry unmodifiableView() {
        return new EventValueRegistryImpl.UnmodifiableView(this);
    }

    public record Flags(Set<Flag> set) {
        public static final Flags DEFAULT = new Flags(Collections.unmodifiableSet(EnumSet.allOf(Flag.class)));
        public static final Flags NONE = new Flags(Collections.unmodifiableSet(EnumSet.noneOf(Flag.class)));

        public static Flags of(Collection<Flag> flags) {
            return new Flags(EnumSet.copyOf(flags));
        }

        public static Flags of(Flag ... flags) {
            return new Flags(EnumSet.noneOf(Flag.class)).with(flags);
        }

        public boolean has(Flag flag) {
            return this.set.contains((Object)flag);
        }

        public Flags with(Flag ... flags) {
            EnumSet<Flag> newSet = EnumSet.copyOf(this.set);
            newSet.addAll(Arrays.asList(flags));
            return new Flags(newSet);
        }

        public Flags without(Flag ... flags) {
            EnumSet<Flag> newSet = EnumSet.copyOf(this.set);
            Arrays.asList(flags).forEach(newSet::remove);
            return new Flags(newSet);
        }
    }

    public static enum Flag {
        FALLBACK_TO_DEFAULT_TIME_STATE,
        ALLOW_CONVERSION;

    }

    public record Resolution<E extends Event, V>(List<EventValue<E, V>> all, boolean errored) {
        public static <E extends Event, V> Resolution<E, V> of(List<EventValue<E, V>> eventValues) {
            return new Resolution<E, V>(eventValues, false);
        }

        public static <E extends Event, V> Resolution<E, V> empty() {
            return new Resolution<E, V>(Collections.emptyList(), false);
        }

        public static <E extends Event, V> Resolution<E, V> error() {
            return new Resolution<E, V>(Collections.emptyList(), true);
        }

        public boolean successful() {
            return !this.all.isEmpty();
        }

        public boolean multiple() {
            return this.all.size() > 1;
        }

        public EventValue<E, V> unique() {
            if (this.all.size() != 1) {
                throw new IllegalStateException("Resolution is not unique (size: " + this.all.size() + ")");
            }
            return this.all.getFirst();
        }

        public EventValue<E, V> uniqueOrNull() {
            if (this.all.size() != 1) {
                return null;
            }
            return this.all.getFirst();
        }

        public Optional<EventValue<E, V>> uniqueOptional() {
            if (this.all.size() != 1) {
                return Optional.empty();
            }
            return Optional.of(this.all.getFirst());
        }

        public EventValue<E, V> any() {
            if (this.all.isEmpty()) {
                throw new IllegalStateException("Resolution is empty");
            }
            return this.all.getFirst();
        }

        public EventValue<E, V> anyOrNull() {
            if (this.all.isEmpty()) {
                return null;
            }
            return this.all.getFirst();
        }

        public Optional<EventValue<E, V>> anyOptional() {
            if (this.all.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(this.all.getFirst());
        }

        public int size() {
            return this.all.size();
        }
    }
}

