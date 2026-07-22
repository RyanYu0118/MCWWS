/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.util.StringMode;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Command")
@Description(value={"Executes a command. This can be useful to use other plugins in triggers.", "If the command is a bungeecord side command, you can use the [bungeecord] option to execute command on the proxy."})
@Example.Examples(value={@Example(value="make player execute command \"/home\""), @Example(value="execute console command \"/say Hello everyone!\""), @Example(value="execute player bungeecord command \"/alert &6Testing Announcement!\"")})
@Since(value={"1.0, 2.8.0 (bungeecord command)"})
public class EffCommand
extends Effect {
    public static final String MESSAGE_CHANNEL = "Message";
    @Nullable
    private Expression<CommandSender> senders;
    private Expression<String> commands;
    private boolean bungeecord;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            this.commands = exprs[0];
            this.senders = exprs[1];
        } else {
            this.senders = exprs[0];
            this.commands = exprs[1];
        }
        this.bungeecord = parseResult.hasTag("bungee");
        if (this.bungeecord && this.senders == null) {
            Skript.error("The commandsenders expression cannot be omitted when using the bungeecord option");
            return false;
        }
        this.commands = VariableString.setStringMode(this.commands, StringMode.COMMAND);
        return true;
    }

    /*
     * WARNING - void declaration
     */
    @Override
    public void execute(Event event) {
        for (String string : this.commands.getArray(event)) {
            void var5_5;
            assert (string != null);
            if (string.startsWith("/")) {
                String string2 = string.substring(1);
            }
            if (this.senders != null) {
                for (CommandSender sender : this.senders.getArray(event)) {
                    if (this.bungeecord) {
                        if (!(sender instanceof Player)) continue;
                        Player player = (Player)sender;
                        Utils.sendPluginMessage(player, "BungeeCord", MESSAGE_CHANNEL, player.getName(), "/" + (String)var5_5);
                        continue;
                    }
                    Skript.dispatchCommand(sender, (String)var5_5);
                }
                continue;
            }
            Skript.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)var5_5);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make " + (this.senders != null ? this.senders.toString(event, debug) : "the console") + " execute " + (this.bungeecord ? "bungeecord " : "") + "command " + this.commands.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffCommand.class, "[execute] [the] [bungee:bungee[cord]] command[s] %strings% [by %-commandsenders%]", "[execute] [the] %commandsenders% [bungee:bungee[cord]] command[s] %strings%", "(let|make) %commandsenders% execute [[the] [bungee:bungee[cord]] command[s]] %strings%");
    }
}

