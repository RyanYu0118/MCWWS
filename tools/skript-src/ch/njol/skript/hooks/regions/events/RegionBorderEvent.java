/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package ch.njol.skript.hooks.regions.events;

import ch.njol.skript.hooks.regions.classes.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RegionBorderEvent
extends Event
implements Cancellable {
    private final Region region;
    final Player player;
    private final boolean enter;
    private boolean cancelled = false;
    private static final HandlerList handlers = new HandlerList();

    public RegionBorderEvent(Region region, Player player, boolean enter) {
        this.region = region;
        this.player = player;
        this.enter = enter;
    }

    public boolean isEntering() {
        return this.enter;
    }

    public Region getRegion() {
        return this.region;
    }

    public Player getPlayer() {
        return this.player;
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

