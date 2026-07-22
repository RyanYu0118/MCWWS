/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror;

import org.bukkit.event.Event;

public abstract class WrappedEvent
extends Event {
    private final Event event;

    protected WrappedEvent(Event event) {
        this.event = event;
    }

    protected WrappedEvent(Event event, boolean isAsynchronous) {
        super(isAsynchronous);
        this.event = event;
    }

    public Event getEvent() {
        if (this.event instanceof WrappedEvent) {
            return ((WrappedEvent)this.event).getEvent();
        }
        return this.event;
    }

    public Event getDirectEvent() {
        return this.event;
    }
}

