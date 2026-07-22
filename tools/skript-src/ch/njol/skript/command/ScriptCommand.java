/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandMap
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.SimpleCommandMap
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.help.GenericCommandHelpTopic
 *  org.bukkit.help.HelpMap
 *  org.bukkit.help.HelpTopic
 *  org.bukkit.help.HelpTopicComparator
 *  org.bukkit.help.IndexHelpTopic
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.command;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.command.Argument;
import ch.njol.skript.command.CommandUsage;
import ch.njol.skript.command.Commands;
import ch.njol.skript.command.ScriptCommandEvent;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.log.Verbosity;
import ch.njol.skript.util.Date;
import ch.njol.skript.util.EmptyStacktraceException;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.variables.HintManager;
import ch.njol.skript.variables.Variables;
import ch.njol.util.StringUtils;
import com.google.common.base.Preconditions;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.help.GenericCommandHelpTopic;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.help.HelpTopicComparator;
import org.bukkit.help.IndexHelpTopic;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentParser;
import org.skriptlang.skript.lang.script.Script;

public class ScriptCommand
implements TabExecutor {
    public static final Message m_executable_by_players = new Message("commands.executable by players");
    public static final Message m_executable_by_console = new Message("commands.executable by console");
    private static final String DEFAULT_PREFIX = "skript";
    final String name;
    private final String label;
    private final List<String> aliases;
    private List<String> activeAliases;
    private String permission;
    private final Expression<? extends Component> permissionMessage;
    private final String description;
    private final String prefix;
    @Nullable
    private final Timespan cooldown;
    private final Expression<? extends Component> cooldownMessage;
    private final String cooldownBypass;
    @Nullable
    private final Expression<String> cooldownStorage;
    final CommandUsage usage;
    private final Trigger trigger;
    private final String pattern;
    private final List<Argument<?>> arguments;
    public static final int PLAYERS = 1;
    public static final int CONSOLE = 2;
    public static final int BOTH = 3;
    final int executableBy;
    private transient PluginCommand bukkitCommand;
    private Map<UUID, Date> lastUsageMap = new HashMap<UUID, Date>();
    @Nullable
    private transient Command overridden = null;
    private transient Map<String, Command> overriddenAliases = new HashMap<String, Command>();
    private transient Collection<HelpTopic> helps = new ArrayList<HelpTopic>();

    public ScriptCommand(Script script, String name, String pattern, List<Argument<?>> arguments, String description, @Nullable String prefix, String usage, List<String> aliases, String permission, @Nullable VariableString permissionMessage, @Nullable Timespan cooldown, @Nullable VariableString cooldownMessage, String cooldownBypass, @Nullable VariableString cooldownStorage, int executableBy, SectionNode node) {
        this(script, name, pattern, arguments, description, prefix, new CommandUsage(null, usage), aliases, permission, permissionMessage, cooldown, cooldownMessage, cooldownBypass, cooldownStorage, executableBy, node);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public ScriptCommand(Script script, String name, String pattern, List<Argument<?>> arguments, String description, @Nullable String prefix, CommandUsage usage, List<String> aliases, String permission, @Nullable VariableString permissionMessage, @Nullable Timespan cooldown, @Nullable VariableString cooldownMessage, String cooldownBypass, @Nullable VariableString cooldownStorage, int executableBy, SectionNode node) {
        Object defaultMsg;
        Preconditions.checkNotNull((Object)name);
        Preconditions.checkNotNull((Object)pattern);
        Preconditions.checkNotNull(arguments);
        Preconditions.checkNotNull((Object)description);
        Preconditions.checkNotNull((Object)usage);
        Preconditions.checkNotNull(aliases);
        Preconditions.checkNotNull((Object)node);
        this.name = name;
        this.label = name.toLowerCase(Locale.ENGLISH);
        this.permission = permission;
        if (permissionMessage == null) {
            defaultMsg = VariableString.newInstance(Language.get("commands.no permission message"));
            assert (defaultMsg != null);
            permissionMessage = defaultMsg;
        }
        this.permissionMessage = permissionMessage.getConvertedExpression(Component.class);
        if (prefix != null) {
            for (Object c : (Object)prefix.toCharArray()) {
                if (Character.isWhitespace((char)c)) {
                    Skript.warning("command /" + name + " has a whitespace in its prefix. Defaulting to 'skript'.");
                    prefix = DEFAULT_PREFIX;
                } else {
                    if (c != 167) continue;
                    Skript.warning("command /" + name + " has a section character in its prefix. Defaulting to 'skript'.");
                    prefix = DEFAULT_PREFIX;
                }
                break;
            }
        } else {
            prefix = DEFAULT_PREFIX;
        }
        this.prefix = prefix;
        this.cooldown = cooldown;
        if (cooldownMessage == null) {
            defaultMsg = VariableString.newInstance(Language.get("commands.cooldown message"));
            assert (defaultMsg != null);
            cooldownMessage = defaultMsg;
        }
        this.cooldownMessage = ((VariableString)cooldownMessage).getConvertedExpression(Component.class);
        this.cooldownBypass = cooldownBypass;
        this.cooldownStorage = cooldownStorage;
        aliases.removeIf(this.label::equalsIgnoreCase);
        this.aliases = aliases;
        this.activeAliases = new ArrayList<String>(aliases);
        TextComponentParser textComponentParser = TextComponentParser.instance();
        this.description = textComponentParser.toLegacyString(textComponentParser.parseSafe(description));
        this.usage = usage;
        this.executableBy = executableBy;
        this.pattern = pattern;
        this.arguments = arguments;
        HintManager hintManager = ParserInstance.get().getHintManager();
        try {
            hintManager.enterScope(false);
            for (Argument<?> argument : arguments) {
                Object hintName = argument.getName();
                if (hintName == null) continue;
                if (!argument.isSingle()) {
                    hintName = (String)hintName + "::*";
                }
                hintManager.set((String)hintName, argument.getType());
            }
            this.trigger = new Trigger(script, "command /" + name, new SimpleEvent(), ScriptLoader.loadItems(node));
            this.trigger.setLineNumber(node.getLine());
        }
        finally {
            hintManager.exitScope();
        }
        this.bukkitCommand = this.setupBukkitCommand();
    }

    private PluginCommand setupBukkitCommand() {
        try {
            Constructor c = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            c.setAccessible(true);
            PluginCommand bukkitCommand = (PluginCommand)c.newInstance(new Object[]{this.name, Skript.getInstance()});
            bukkitCommand.setAliases(this.aliases);
            bukkitCommand.setDescription(this.description);
            bukkitCommand.setLabel(this.label);
            bukkitCommand.setPermission(this.permission);
            bukkitCommand.setUsage(this.usage.getUsage());
            bukkitCommand.setExecutor((CommandExecutor)this);
            return bukkitCommand;
        }
        catch (Exception e) {
            Skript.outdatedError(e);
            throw new EmptyStacktraceException();
        }
    }

    public boolean onCommand(@Nullable CommandSender sender, @Nullable Command command, @Nullable String label, @Nullable String[] args) {
        if (sender == null || label == null || args == null) {
            return false;
        }
        this.execute(sender, label, StringUtils.join(args, " "));
        return true;
    }

    public boolean execute(CommandSender sender, String commandLabel, String rest) {
        ScriptCommandEvent event;
        if (sender instanceof Player) {
            if ((this.executableBy & 1) == 0) {
                sender.sendMessage(String.valueOf(m_executable_by_console));
                return false;
            }
        } else if ((this.executableBy & 2) == 0) {
            sender.sendMessage(String.valueOf(m_executable_by_players));
            return false;
        }
        if (!this.checkPermissions(sender, event = new ScriptCommandEvent(this, sender, commandLabel, rest))) {
            return false;
        }
        if (sender instanceof Player && this.cooldown != null) {
            Player player = (Player)sender;
            UUID uuid = player.getUniqueId();
            if (!this.cooldownBypass.isEmpty() && player.hasPermission(this.cooldownBypass)) {
                this.setLastUsage(uuid, event, null);
            } else if (this.getLastUsage(uuid, event) != null) {
                if (this.getRemainingMilliseconds(uuid, event) <= 0L) {
                    if (!SkriptConfig.keepLastUsageDates.value().booleanValue()) {
                        this.setLastUsage(uuid, event, null);
                    }
                } else {
                    Component msg = this.cooldownMessage.getSingle(event);
                    if (msg != null) {
                        sender.sendMessage(msg);
                    }
                    return false;
                }
            }
        }
        Runnable runnable = () -> {
            Date lastUsage;
            Date previousLastUsage = null;
            if (sender instanceof Player) {
                previousLastUsage = this.getLastUsage(((Player)sender).getUniqueId(), event);
            }
            this.execute2(event, sender, commandLabel, rest);
            if (sender instanceof Player && !event.isCooldownCancelled() && Objects.equals(lastUsage = this.getLastUsage(((Player)sender).getUniqueId(), event), previousLastUsage)) {
                this.setLastUsage(((Player)sender).getUniqueId(), event, new Date());
            }
        };
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), runnable);
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    boolean execute2(ScriptCommandEvent event, CommandSender sender, String commandLabel, String rest) {
        ParseLogHandler log = SkriptLogger.startParseLogHandler();
        try {
            boolean ok = SkriptParser.parseArguments(rest, this, event);
            if (!ok) {
                LogEntry e = log.getError();
                if (e != null) {
                    sender.sendMessage(String.valueOf(ChatColor.DARK_RED) + e.toString());
                }
                sender.sendMessage(this.usage.getUsage(event));
                log.clear();
                boolean bl = false;
                return bl;
            }
            log.clearError();
        }
        finally {
            log.stop();
        }
        if (Skript.log(Verbosity.VERY_HIGH)) {
            Skript.info("# /" + this.name + " " + rest);
        }
        long startTrigger = System.nanoTime();
        if (!this.trigger.execute(event)) {
            sender.sendMessage(Commands.m_internal_error.toString());
        }
        if (Skript.log(Verbosity.VERY_HIGH)) {
            Skript.info("# " + this.name + " took " + 1.0 * (double)(System.nanoTime() - startTrigger) / 1000000.0 + " milliseconds");
        }
        return true;
    }

    public boolean checkPermissions(CommandSender sender, String commandLabel, String arguments) {
        return this.checkPermissions(sender, new ScriptCommandEvent(this, sender, commandLabel, arguments));
    }

    public boolean checkPermissions(CommandSender sender, Event event) {
        if (!this.permission.isEmpty() && !sender.hasPermission(this.permission)) {
            Component message = this.permissionMessage.getSingle(event);
            assert (message != null);
            sender.sendMessage(message);
            return false;
        }
        return true;
    }

    public void sendHelp(CommandSender sender) {
        if (!this.description.isEmpty()) {
            sender.sendMessage(this.description);
        }
        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "Usage" + String.valueOf(ChatColor.RESET) + ": " + this.usage.getUsage());
    }

    public List<Argument<?>> getArguments() {
        return this.arguments;
    }

    public String getPattern() {
        return this.pattern;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void register(SimpleCommandMap commandMap, Map<String, Command> knownCommands, @Nullable Set<String> aliases) {
        SimpleCommandMap simpleCommandMap = commandMap;
        synchronized (simpleCommandMap) {
            this.overriddenAliases.clear();
            this.overridden = knownCommands.put(this.label, (Command)this.bukkitCommand);
            if (aliases != null) {
                aliases.remove(this.label);
            }
            Iterator<String> as = this.activeAliases.iterator();
            while (as.hasNext()) {
                String lowerAlias = as.next().toLowerCase(Locale.ENGLISH);
                if (knownCommands.containsKey(lowerAlias) && (aliases == null || !aliases.contains(lowerAlias))) {
                    as.remove();
                    continue;
                }
                this.overriddenAliases.put(lowerAlias, knownCommands.put(lowerAlias, (Command)this.bukkitCommand));
                if (aliases == null) continue;
                aliases.add(lowerAlias);
            }
            this.bukkitCommand.setAliases(this.activeAliases);
            commandMap.register(this.prefix, (Command)this.bukkitCommand);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void unregister(SimpleCommandMap commandMap, Map<String, Command> knownCommands, @Nullable Set<String> aliases) {
        SimpleCommandMap simpleCommandMap = commandMap;
        synchronized (simpleCommandMap) {
            knownCommands.remove(this.label);
            knownCommands.remove(this.prefix + ":" + this.label);
            if (aliases != null) {
                aliases.removeAll(this.activeAliases);
            }
            for (String string : this.activeAliases) {
                knownCommands.remove(string);
                knownCommands.remove(this.prefix + ":" + string);
            }
            this.activeAliases = new ArrayList<String>(this.aliases);
            this.bukkitCommand.unregister((CommandMap)commandMap);
            this.bukkitCommand.setAliases(this.aliases);
            if (this.overridden != null) {
                knownCommands.put(this.label, this.overridden);
                this.overridden = null;
            }
            for (Map.Entry entry : this.overriddenAliases.entrySet()) {
                if (entry.getValue() == null) continue;
                knownCommands.put((String)entry.getKey(), (Command)entry.getValue());
                if (aliases == null) continue;
                aliases.add((String)entry.getKey());
            }
            this.overriddenAliases.clear();
        }
    }

    public void registerHelp() {
        this.helps.clear();
        HelpMap help = Bukkit.getHelpMap();
        GenericCommandHelpTopic t = new GenericCommandHelpTopic((Command)this.bukkitCommand);
        help.addTopic((HelpTopic)t);
        this.helps.add((HelpTopic)t);
        HelpTopic aliases = help.getHelpTopic("Aliases");
        if (aliases instanceof IndexHelpTopic) {
            aliases.getFullText((CommandSender)Bukkit.getConsoleSender());
            try {
                Field topics = IndexHelpTopic.class.getDeclaredField("allTopics");
                topics.setAccessible(true);
                ArrayList<Commands.CommandAliasHelpTopic> as = new ArrayList<Commands.CommandAliasHelpTopic>((Collection)topics.get(aliases));
                for (String alias : this.activeAliases) {
                    Commands.CommandAliasHelpTopic at = new Commands.CommandAliasHelpTopic("/" + alias, "/" + this.getLabel(), help);
                    as.add(at);
                    this.helps.add(at);
                }
                Collections.sort(as, HelpTopicComparator.helpTopicComparatorInstance());
                topics.set(aliases, as);
            }
            catch (Exception e) {
                Skript.outdatedError(e);
            }
        }
    }

    public void unregisterHelp() {
        Bukkit.getHelpMap().getHelpTopics().removeAll(this.helps);
        HelpTopic aliases = Bukkit.getHelpMap().getHelpTopic("Aliases");
        if (aliases != null && aliases instanceof IndexHelpTopic) {
            try {
                Field topics = IndexHelpTopic.class.getDeclaredField("allTopics");
                topics.setAccessible(true);
                ArrayList as = new ArrayList((Collection)topics.get(aliases));
                as.removeAll(this.helps);
                topics.set(aliases, as);
            }
            catch (Exception e) {
                Skript.outdatedError(e);
            }
        }
        this.helps.clear();
    }

    public String getName() {
        return this.name;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getLabel() {
        return this.label;
    }

    @Nullable
    public Timespan getCooldown() {
        return this.cooldown;
    }

    @Nullable
    private String getStorageVariableName(Event event) {
        assert (this.cooldownStorage != null);
        String variableString = this.cooldownStorage.getSingle(event);
        if (variableString == null) {
            return null;
        }
        if (variableString.startsWith("{")) {
            variableString = variableString.substring(1);
        }
        if (variableString.endsWith("}")) {
            variableString = variableString.substring(0, variableString.length() - 1);
        }
        return variableString;
    }

    @Nullable
    public Date getLastUsage(UUID uuid, Event event) {
        if (this.cooldownStorage == null) {
            return this.lastUsageMap.get(uuid);
        }
        String name = this.getStorageVariableName(event);
        assert (name != null);
        Object variable = Variables.getVariable(name, null, false);
        if (variable != null && !(variable instanceof Date)) {
            Skript.warning("Variable {" + name + "} was not a date! You may be using this variable elsewhere. This warning is letting you know that this variable is now overridden for the command storage.");
            return null;
        }
        return (Date)variable;
    }

    public void setLastUsage(UUID uuid, Event event, @Nullable Date date) {
        if (this.cooldownStorage != null) {
            String name = this.getStorageVariableName(event);
            assert (name != null);
            Variables.setVariable(name, date, null, false);
        } else if (date == null) {
            this.lastUsageMap.remove(uuid);
        } else {
            this.lastUsageMap.put(uuid, date);
        }
    }

    public long getRemainingMilliseconds(UUID uuid, Event event) {
        Date lastUsage = this.getLastUsage(uuid, event);
        if (lastUsage == null) {
            return 0L;
        }
        Timespan cooldown = this.cooldown;
        assert (cooldown != null);
        long remaining = cooldown.getAs(Timespan.TimePeriod.MILLISECOND) - this.getElapsedMilliseconds(uuid, event);
        if (remaining < 0L) {
            remaining = 0L;
        }
        return remaining;
    }

    public void setRemainingMilliseconds(UUID uuid, Event event, long milliseconds) {
        Timespan cooldown = this.cooldown;
        assert (cooldown != null);
        long cooldownMs = cooldown.getAs(Timespan.TimePeriod.MILLISECOND);
        if (milliseconds > cooldownMs) {
            milliseconds = cooldownMs;
        }
        this.setElapsedMilliSeconds(uuid, event, cooldownMs - milliseconds);
    }

    public long getElapsedMilliseconds(UUID uuid, Event event) {
        Date lastUsage = this.getLastUsage(uuid, event);
        return lastUsage == null ? 0L : Date.now().getTime() - lastUsage.getTime();
    }

    public void setElapsedMilliSeconds(UUID uuid, Event event, long milliseconds) {
        Date date = Date.now();
        date.subtract(new Timespan(milliseconds));
        this.setLastUsage(uuid, event, date);
    }

    public String getCooldownBypass() {
        return this.cooldownBypass;
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    public List<String> getActiveAliases() {
        return this.activeAliases;
    }

    public PluginCommand getBukkitCommand() {
        return this.bukkitCommand;
    }

    @Nullable
    public Script getScript() {
        return this.trigger.getScript();
    }

    @Nullable
    public List<String> onTabComplete(@Nullable CommandSender sender, @Nullable Command command, @Nullable String alias, @Nullable String[] args) {
        assert (args != null);
        int argIndex = args.length - 1;
        if (argIndex >= this.arguments.size()) {
            return Collections.emptyList();
        }
        Argument<?> arg = this.arguments.get(argIndex);
        Class<?> argType = arg.getType();
        if (argType.equals(Player.class) || argType.equals(OfflinePlayer.class)) {
            return null;
        }
        return Collections.emptyList();
    }
}

