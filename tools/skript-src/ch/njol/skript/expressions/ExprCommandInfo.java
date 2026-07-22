/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandMap
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.SimpleCommandMap
 *  org.bukkit.command.defaults.BukkitCommand
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.server.ServerCommandEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.command.Commands;
import ch.njol.skript.command.ScriptCommand;
import ch.njol.skript.command.ScriptCommandEvent;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Command Info")
@Description(value={"Get information about a command."})
@Example.Examples(value={@Example(value="main command label of command \"skript\""), @Example(value="description of command \"help\""), @Example(value="label of command \"pl\""), @Example(value="usage of command \"help\""), @Example(value="aliases of command \"bukkit:help\""), @Example(value="permission of command \"/op\""), @Example(value="command \"op\"'s permission message"), @Example(value="command \"sk\"'s plugin owner"), @Example(value="command /greet <player>:\n\tusage: /greet <target>\n\ttrigger:\n\t\tif arg-1 is sender:\n\t\t\tsend \"&cYou can't greet yourself! Usage: %the usage%\"\n\t\t\tstop\n\t\tsend \"%sender% greets you!\" to arg-1\n\t\tsend \"You greeted %arg-1%!\"\n")})
@Since(value={"2.6"})
public class ExprCommandInfo
extends SimpleExpression<String> {
    private InfoType type;
    @Nullable
    private Expression<String> commandName;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.commandName = exprs[0];
        if (this.commandName == null && !this.getParser().isCurrentEvent(ScriptCommandEvent.class, PlayerCommandPreprocessEvent.class, ServerCommandEvent.class)) {
            Skript.error("There's no command in " + Utils.a(this.getParser().getCurrentEventName()) + " event. Please provide a command");
            return false;
        }
        this.type = InfoType.values()[Math.floorDiv(matchedPattern, 2)];
        return true;
    }

    @Nullable
    protected String[] get(Event event) {
        Command[] commands = this.getCommands(event);
        if (commands == null) {
            return new String[0];
        }
        if (this.type == InfoType.ALIASES) {
            ArrayList<String> result = new ArrayList<String>();
            for (Command command : commands) {
                result.addAll(ExprCommandInfo.getAliases(command));
            }
            return result.toArray(new String[0]);
        }
        String[] result = new String[commands.length];
        for (int i = 0; i < commands.length; ++i) {
            result[i] = this.type.function.apply(commands[i]);
        }
        return result;
    }

    @Override
    public boolean isSingle() {
        return this.type != InfoType.ALIASES && (this.commandName == null || this.commandName.isSingle());
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the " + this.type.name().toLowerCase(Locale.ENGLISH).replace("_", " ") + (String)(this.commandName == null ? "" : " of command " + this.commandName.toString(event, debug));
    }

    @Nullable
    private Command[] getCommands(Event event) {
        Command[] commandArray;
        String commandName;
        if (event instanceof ScriptCommandEvent && this.commandName == null) {
            return new Command[]{((ScriptCommandEvent)event).getScriptCommand().getBukkitCommand()};
        }
        SimpleCommandMap map = Commands.getCommandMap();
        if (map == null) {
            return null;
        }
        if (this.commandName != null) {
            return (Command[])this.commandName.stream(event).map(arg_0 -> ((CommandMap)map).getCommand(arg_0)).filter(Objects::nonNull).toArray(Command[]::new);
        }
        if (event instanceof ServerCommandEvent) {
            commandName = ((ServerCommandEvent)event).getCommand();
        } else if (event instanceof PlayerCommandPreprocessEvent) {
            commandName = ((PlayerCommandPreprocessEvent)event).getMessage().substring(1);
        } else {
            return null;
        }
        commandName = commandName.split(":")[0];
        Command command = map.getCommand(commandName);
        if (command != null) {
            Command[] commandArray2 = new Command[1];
            commandArray = commandArray2;
            commandArray2[0] = command;
        } else {
            commandArray = null;
        }
        return commandArray;
    }

    private static List<String> getAliases(Command command) {
        if (!(command instanceof PluginCommand) || ((PluginCommand)command).getPlugin() != Skript.getInstance()) {
            return command.getAliases();
        }
        ScriptCommand scriptCommand = Commands.getScriptCommand(command.getName());
        return scriptCommand == null ? command.getAliases() : scriptCommand.getAliases();
    }

    static {
        Skript.registerExpression(ExprCommandInfo.class, String.class, ExpressionType.PROPERTY, "[the] main command [label|name] [of [[the] command[s] %-strings%]]", "command[s] %strings%'[s] main command [label|name]", "[the] description [of [[the] command[s] %-strings%]]", "command[s] %strings%'[s] description", "[the] label [of [[the] command[s] %-strings%]]", "command[s] %strings%'[s] label", "[the] usage [of [[the] command[s] %-strings%]]", "command[s] %strings%'[s] usage", "[(all|the|all [of] the)] aliases [of [[the] command[s] %-strings%]]", "command[s] %strings%'[s] aliases", "[the] permission [of [[the] command[s] %-strings%]]", "command[s] %strings%'[s] permission", "[the] permission message [of [[the] command[s] %-strings%]]", "command[s] %strings%'[s] permission message", "[the] plugin [owner] [of [[the] command[s] %-strings%]]", "command[s] %strings%'[s] plugin [owner]");
    }

    private static enum InfoType {
        NAME(Command::getName),
        DESCRIPTION(Command::getDescription),
        LABEL(Command::getLabel),
        USAGE(Command::getUsage),
        ALIASES(null),
        PERMISSION(Command::getPermission),
        PERMISSION_MESSAGE(Command::getPermissionMessage),
        PLUGIN(command -> {
            if (command instanceof PluginCommand) {
                return ((PluginCommand)command).getPlugin().getName();
            }
            if (command instanceof BukkitCommand) {
                return "Bukkit";
            }
            if (command.getClass().getPackage().getName().startsWith("org.spigot")) {
                return "Spigot";
            }
            if (command.getClass().getPackage().getName().startsWith("com.destroystokyo.paper")) {
                return "Paper";
            }
            return "Unknown";
        });

        @Nullable
        private final Function<Command, String> function;

        private InfoType(Function<Command, String> function) {
            this.function = function;
        }
    }
}

