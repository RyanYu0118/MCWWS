/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.command;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

public class CommandEvent
extends Event {
    private final CommandSender sender;
    String command;
    @Nullable
    private final String[] args;
    private static final HandlerList handlers = new HandlerList();

    public CommandEvent(CommandSender sender, String command, @Nullable String[] args) {
        this.sender = sender;
        this.command = command;
        this.args = args;
    }

    public CommandSender getSender() {
        return this.sender;
    }

    public String getCommand() {
        return this.command;
    }

    @Nullable
    public String[] getArgs() {
        return this.args;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

