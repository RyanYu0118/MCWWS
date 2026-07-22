/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.ConsoleCommandSender
 *  org.bukkit.command.SimpleCommandMap
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.server.ServerCommandEvent
 *  org.bukkit.help.HelpMap
 *  org.bukkit.help.HelpTopic
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.SimplePluginManager
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.command;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.command.Argument;
import ch.njol.skript.command.EffectCommandEvent;
import ch.njol.skript.command.ScriptCommand;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Message;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.variables.Variables;
import com.google.common.base.Preconditions;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentParser;
import org.skriptlang.skript.lang.script.Script;

public abstract class Commands {
    public static final ArgsMessage m_too_many_arguments = new ArgsMessage("commands.too many arguments");
    public static final Message m_internal_error = new Message("commands.internal error");
    public static final Message m_correct_usage = new Message("commands.correct usage");
    public static final int CONVERTER_NO_COMMAND_ARGUMENTS = 8;
    private static final Map<String, ScriptCommand> commands = new HashMap<String, ScriptCommand>();
    @Nullable
    private static SimpleCommandMap commandMap = null;
    @Nullable
    private static Map<String, Command> cmKnownCommands;
    @Nullable
    private static Set<String> cmAliases;
    @Nullable
    public static List<Argument<?>> currentArguments;
    private static final Pattern escape;
    private static final Pattern unescape;
    private static final Listener commandListener;
    private static boolean registeredListeners;

    public static Set<String> getScriptCommands() {
        return commands.keySet();
    }

    @Nullable
    public static SimpleCommandMap getCommandMap() {
        return commandMap;
    }

    private static void init() {
        try {
            if (Bukkit.getPluginManager() instanceof SimplePluginManager) {
                Field commandMapField = SimplePluginManager.class.getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                commandMap = (SimpleCommandMap)commandMapField.get(Bukkit.getPluginManager());
                Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                cmKnownCommands = (Map)knownCommandsField.get(commandMap);
                try {
                    Field aliasesField = SimpleCommandMap.class.getDeclaredField("aliases");
                    aliasesField.setAccessible(true);
                    cmAliases = (Set)aliasesField.get(commandMap);
                }
                catch (NoSuchFieldException noSuchFieldException) {}
            }
        }
        catch (SecurityException e) {
            Skript.error("Please disable the security manager");
            commandMap = null;
        }
        catch (Exception e) {
            Skript.outdatedError(e);
            commandMap = null;
        }
    }

    public static String escape(String string) {
        return escape.matcher(string).replaceAll("\\\\$0");
    }

    public static String unescape(String string) {
        return unescape.matcher(string).replaceAll("$0");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean handleEffectCommand(CommandSender sender, String command) {
        if (!(Skript.testing() || sender instanceof ConsoleCommandSender || sender.hasPermission("skript.effectcommands") || SkriptConfig.allowOpsToUseEffectCommands.value().booleanValue() && sender.isOp())) {
            return false;
        }
        try {
            command = ((String)command).substring(SkriptConfig.effectCommandToken.value().length()).trim();
            RetainingLogHandler log = SkriptLogger.startRetainingLog();
            try {
                EffectCommandEvent effectCommand = new EffectCommandEvent(sender, (String)command);
                Bukkit.getPluginManager().callEvent((Event)effectCommand);
                command = effectCommand.getCommand();
                ParserInstance parserInstance = ParserInstance.get();
                parserInstance.setCurrentEvent("effect command", EffectCommandEvent.class);
                Effect effect = Effect.parse((String)command, null);
                parserInstance.deleteCurrentEvent();
                TextComponentParser textParser = TextComponentParser.instance();
                if (effect != null) {
                    log.clear();
                    log.printLog();
                    if (!effectCommand.isCancelled()) {
                        sender.sendMessage(textParser.parse("<gray>Executing '" + textParser.escape((String)command) + "'"));
                        if (SkriptConfig.logEffectCommands.value().booleanValue() && !(sender instanceof ConsoleCommandSender)) {
                            Skript.info(sender.getName() + " issued effect command: " + textParser.escape((String)command));
                        }
                        TriggerItem.walk(effect, effectCommand);
                        Variables.removeLocals(effectCommand);
                    } else {
                        sender.sendMessage(textParser.parse("<red>Your effect command '" + textParser.escape((String)command) + "' was cancelled."));
                    }
                } else {
                    if (sender == Bukkit.getConsoleSender()) {
                        SkriptLogger.LOGGER.severe("Error in: " + (String)command);
                    } else {
                        sender.sendMessage(textParser.parse("<red>Error in: <gray>" + textParser.escape((String)command)));
                    }
                    log.printErrors(sender, "(No specific information is available)");
                }
            }
            finally {
                log.stop();
            }
            return true;
        }
        catch (Exception e) {
            Skript.exception((Throwable)e, "Unexpected error while executing effect command '" + TextComponentParser.instance().escape((String)command) + "' by '" + sender.getName() + "'");
            sender.sendRichMessage("<red>An internal error occurred while executing this effect. Please refer to the server log for details.");
            return true;
        }
    }

    @Nullable
    public static ScriptCommand getScriptCommand(String key) {
        return commands.get(key);
    }

    @Deprecated(since="2.8.0", forRemoval=true)
    public static boolean skriptCommandExists(String command) {
        return Commands.scriptCommandExists(command);
    }

    public static boolean scriptCommandExists(String command) {
        ScriptCommand scriptCommand = commands.get(command);
        return scriptCommand != null && scriptCommand.getName().equals(command);
    }

    public static void registerCommand(ScriptCommand command) {
        ScriptCommand existingCommand = commands.get(command.getLabel());
        if (existingCommand != null && existingCommand.getLabel().equals(command.getLabel())) {
            Script script = existingCommand.getScript();
            Skript.error("A command with the name /" + existingCommand.getName() + " is already defined" + (String)(script != null ? " in " + script.getConfig().getFileName() : ""));
            return;
        }
        if (commandMap != null) {
            assert (cmKnownCommands != null);
            command.register(commandMap, cmKnownCommands, cmAliases);
        }
        commands.put(command.getLabel(), command);
        for (String alias : command.getActiveAliases()) {
            commands.put(alias.toLowerCase(Locale.ENGLISH), command);
        }
        command.registerHelp();
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    public static int unregisterCommands(File script) {
        int numCommands = 0;
        for (ScriptCommand c : new ArrayList<ScriptCommand>(commands.values())) {
            if (c.getScript() == null || !c.getScript().equals(ScriptLoader.getScript(script))) continue;
            ++numCommands;
            Commands.unregisterCommand(c);
        }
        return numCommands;
    }

    public static void unregisterCommand(ScriptCommand scriptCommand) {
        scriptCommand.unregisterHelp();
        if (commandMap != null) {
            assert (cmKnownCommands != null);
            scriptCommand.unregister(commandMap, cmKnownCommands, cmAliases);
        }
        commands.values().removeIf(command -> command == scriptCommand);
    }

    public static void registerListeners() {
        if (!registeredListeners) {
            Bukkit.getPluginManager().registerEvents(commandListener, (Plugin)Skript.getInstance());
            Bukkit.getPluginManager().registerEvents(new Listener(){

                @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
                public void onPlayerChat(AsyncPlayerChatEvent event) {
                    if (!SkriptConfig.enableEffectCommands.value().booleanValue() && !Skript.testing() || !event.getMessage().startsWith(SkriptConfig.effectCommandToken.value())) {
                        return;
                    }
                    if (!event.isAsynchronous()) {
                        if (Commands.handleEffectCommand((CommandSender)event.getPlayer(), event.getMessage())) {
                            event.setCancelled(true);
                        }
                    } else {
                        Future f = Bukkit.getScheduler().callSyncMethod((Plugin)Skript.getInstance(), () -> Commands.handleEffectCommand((CommandSender)event.getPlayer(), event.getMessage()));
                        try {
                            while (true) {
                                try {
                                    if (((Boolean)f.get()).booleanValue()) {
                                        event.setCancelled(true);
                                    }
                                }
                                catch (InterruptedException interruptedException) {
                                    continue;
                                }
                                break;
                            }
                        }
                        catch (ExecutionException e) {
                            Skript.exception((Throwable)e, new String[0]);
                        }
                    }
                }
            }, (Plugin)Skript.getInstance());
            registeredListeners = true;
        }
    }

    static {
        Commands.init();
        currentArguments = null;
        escape = Pattern.compile("[" + Pattern.quote("(|)<>%\\") + "]");
        unescape = Pattern.compile("\\\\[" + Pattern.quote("(|)<>%\\") + "]");
        commandListener = new Listener(){

            @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
            public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
                CharSequence[] cmd = event.getMessage().substring(1).split("\\s+", 2);
                String label = cmd[0].toLowerCase(Locale.ENGLISH);
                String arguments = cmd.length == 1 ? "" : cmd[1];
                ScriptCommand command = commands.get(label);
                if (command != null) {
                    if (!command.checkPermissions((CommandSender)event.getPlayer(), label, arguments)) {
                        event.setCancelled(true);
                    }
                    if (SkriptConfig.caseInsensitiveCommands.value().booleanValue()) {
                        cmd[0] = event.getMessage().charAt(0) + label;
                        event.setMessage(String.join((CharSequence)" ", cmd));
                    }
                }
            }

            @EventHandler(priority=EventPriority.HIGHEST)
            public void onServerCommand(ServerCommandEvent event) {
                if (event.getCommand().isEmpty() || event.isCancelled()) {
                    return;
                }
                if ((Skript.testing() || SkriptConfig.enableEffectCommands.value().booleanValue()) && event.getCommand().startsWith(SkriptConfig.effectCommandToken.value()) && Commands.handleEffectCommand(event.getSender(), event.getCommand())) {
                    event.setCancelled(true);
                }
            }
        };
        registeredListeners = false;
    }

    public static final class CommandAliasHelpTopic
    extends HelpTopic {
        private final String aliasFor;
        private final HelpMap helpMap;

        public CommandAliasHelpTopic(String alias, String aliasFor, HelpMap helpMap) {
            this.aliasFor = aliasFor.startsWith("/") ? aliasFor : "/" + aliasFor;
            this.helpMap = helpMap;
            this.name = alias.startsWith("/") ? alias : "/" + alias;
            Preconditions.checkState((!this.name.equals(this.aliasFor) ? 1 : 0) != 0, (Object)("Command " + this.name + " cannot be alias for itself"));
            this.shortText = String.valueOf(ChatColor.YELLOW) + "Alias for " + String.valueOf(ChatColor.WHITE) + this.aliasFor;
        }

        @NotNull
        public String getFullText(CommandSender forWho) {
            StringBuilder fullText = new StringBuilder(this.shortText);
            HelpTopic aliasForTopic = this.helpMap.getHelpTopic(this.aliasFor);
            if (aliasForTopic != null) {
                fullText.append("\n");
                fullText.append(aliasForTopic.getFullText(forWho));
            }
            return String.valueOf(fullText);
        }

        public boolean canSee(CommandSender commandSender) {
            if (this.amendedPermission != null) {
                return commandSender.hasPermission(this.amendedPermission);
            }
            HelpTopic aliasForTopic = this.helpMap.getHelpTopic(this.aliasFor);
            return aliasForTopic != null && aliasForTopic.canSee(commandSender);
        }
    }
}

