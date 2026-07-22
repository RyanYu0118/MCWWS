/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package ch.njol.skript.lang.function;

import ch.njol.skript.lang.function.Function;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class FunctionEvent<T>
extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Function<? extends T> function;

    public FunctionEvent(Function<? extends T> function) {
        this.function = function;
    }

    public FunctionEvent(org.skriptlang.skript.common.function.Function<? extends T> function) {
        this.function = (Function)function;
    }

    public Function<? extends T> getFunction() {
        return this.function;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

