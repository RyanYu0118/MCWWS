/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package ch.njol.skript.events.bukkit;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ExperienceSpawnEvent
extends Event
implements Cancellable {
    private int exp;
    private final Location location;
    private boolean cancelled = false;
    private static final HandlerList handlers = new HandlerList();

    public ExperienceSpawnEvent(int exp, Location location) {
        this.exp = exp;
        this.location = location;
    }

    public int getSpawnedXP() {
        return this.exp;
    }

    public void setSpawnedXP(int xp) {
        this.exp = Math.max(0, xp);
    }

    public Location getLocation() {
        return this.location;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

