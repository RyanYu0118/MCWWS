/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.HandlerList
 */
package ch.njol.skript.command;

import ch.njol.skript.command.CommandEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class EffectCommandEvent
extends CommandEvent
implements Cancellable {
    private boolean cancelled;
    private static final HandlerList handlers = new HandlerList();

    public EffectCommandEvent(CommandSender sender, String command) {
        super(sender, command, new String[0]);
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

