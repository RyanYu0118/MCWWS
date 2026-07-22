/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.HandlerList
 */
package ch.njol.skript.command;

import ch.njol.skript.command.CommandEvent;
import ch.njol.skript.command.ScriptCommand;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.util.Date;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class ScriptCommandEvent
extends CommandEvent {
    private final ScriptCommand scriptCommand;
    private final String commandLabel;
    private final String rest;
    private final Date executionDate = new Date();
    private boolean cooldownCancelled;
    private static final HandlerList handlers = new HandlerList();

    public ScriptCommandEvent(ScriptCommand scriptCommand, CommandSender sender, String commandLabel, String rest) {
        super(sender, scriptCommand.getLabel(), rest.split(" "));
        this.scriptCommand = scriptCommand;
        this.commandLabel = commandLabel;
        this.rest = rest;
    }

    public ScriptCommand getScriptCommand() {
        return this.scriptCommand;
    }

    public String getCommandLabel() {
        return this.commandLabel;
    }

    public String getArgsString() {
        return this.rest;
    }

    public boolean isCooldownCancelled() {
        return this.cooldownCancelled;
    }

    public void setCooldownCancelled(boolean cooldownCancelled) {
        if (Delay.isDelayed(this)) {
            CommandSender sender = this.getSender();
            if (sender instanceof Player) {
                Date date = cooldownCancelled ? null : this.executionDate;
                this.scriptCommand.setLastUsage(((Player)sender).getUniqueId(), this, date);
            }
        } else {
            this.cooldownCancelled = cooldownCancelled;
        }
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

