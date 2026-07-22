/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package ch.njol.skript.lang.util;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class ContextlessEvent
extends Event {
    private ContextlessEvent() {
    }

    public static ContextlessEvent get() {
        return new ContextlessEvent();
    }

    @NotNull
    public HandlerList getHandlers() {
        throw new IllegalStateException();
    }
}

