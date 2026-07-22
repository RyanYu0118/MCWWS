/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.HandlerList
 */
package ch.njol.skript.events.bukkit;

import ch.njol.skript.events.bukkit.ScheduledEvent;
import org.bukkit.event.HandlerList;

public class ScheduledNoWorldEvent
extends ScheduledEvent {
    private static final HandlerList handlers = new HandlerList();

    public ScheduledNoWorldEvent() {
        super(null);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

