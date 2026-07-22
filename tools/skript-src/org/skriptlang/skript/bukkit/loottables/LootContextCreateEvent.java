/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package org.skriptlang.skript.bukkit.loottables;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.skriptlang.skript.bukkit.loottables.LootContextWrapper;

public class LootContextCreateEvent
extends Event {
    private final LootContextWrapper contextWrapper;

    public LootContextCreateEvent(LootContextWrapper context) {
        this.contextWrapper = context;
    }

    public LootContextWrapper getContextWrapper() {
        return this.contextWrapper;
    }

    public HandlerList getHandlers() {
        throw new UnsupportedOperationException();
    }
}

