/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.ImmutableSet$Builder
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.util.event;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.util.event.Event;

public class EventRegistry<E extends Event> {
    private final Set<E> events = ConcurrentHashMap.newKeySet();

    public void register(E event) {
        this.events.add(event);
    }

    public <T extends E> void register(Class<T> eventType, T event) {
        this.events.add(event);
    }

    public void unregister(E event) {
        this.events.remove(event);
    }

    public @Unmodifiable Set<E> events() {
        return ImmutableSet.copyOf(this.events);
    }

    public <T extends E> @Unmodifiable Set<T> events(Class<T> type) {
        ImmutableSet.Builder builder = ImmutableSet.builder();
        this.events.stream().filter(event -> type.isAssignableFrom(event.getClass())).forEach(e -> builder.add(e));
        return builder.build();
    }
}

