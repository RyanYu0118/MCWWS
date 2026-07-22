/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.structures;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.CommandReloader;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.command.Argument;
import ch.njol.skript.command.CommandUsage;
import ch.njol.skript.command.Commands;
import ch.njol.skript.command.ScriptCommand;
import ch.njol.skript.command.ScriptCommandEvent;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.StringMode;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.util.Utils;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;
import org.skriptlang.skript.lang.entry.util.LiteralEntryData;
import org.skriptlang.skript.lang.entry.util.VariableStringEntryData;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;

@Name(value="Command")
@Description(value={"Used for registering custom commands."})
@Example(value="command /broadcast <string>:\n\tusage: A command for broadcasting a message to all players.\n\tpermission: skript.command.broadcast\n\tpermission message: You don't have permission to broadcast messages\n\taliases: /bc\n\texecutable by: players and console\n\tcooldown: 15 seconds\n\tcooldown message: You last broadcast a message %elapsed time% ago. You can broadcast another message in %remaining time%.\n\tcooldown bypass: skript.command.broadcast.admin\n\tcooldown storage: {cooldown::%player%}\n\ttrigger:\n\t\tbroadcast the argument\n")
@Since(value={"1.0"})
public class StructCommand
extends Structure {
    private static final boolean DELAY_COMMAND_SYNCING = Skript.classExists("io.papermc.paper.command.brigadier.Commands");
    public static final Structure.Priority PRIORITY = new Structure.Priority(500);
    private static final Pattern COMMAND_PATTERN = Pattern.compile("(?i)^command\\s+/?(\\S+)\\s*(\\s+(.+))?$");
    private static final Pattern ARGUMENT_PATTERN = Pattern.compile("<\\s*(?:([^>]+?)\\s*:\\s*)?(.+?)\\s*(?:=\\s*([^\"]*?(?:\"[^\"]*?\"[^\"]*?)*?))?\\s*>");
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("(?<!\\\\)%-?(.+?)%");
    private static final AtomicBoolean SYNC_COMMANDS = new AtomicBoolean();
    private EntryContainer entryContainer;
    @Nullable
    private ScriptCommand scriptCommand;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        assert (entryContainer != null);
        this.entryContainer = entryContainer;
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean load() {
        String cooldownBypass;
        this.getParser().setCurrentEvent("command", ScriptCommandEvent.class);
        String fullCommand = this.entryContainer.getSource().getKey();
        assert (fullCommand != null);
        fullCommand = ScriptLoader.replaceOptions(fullCommand);
        int level = 0;
        for (int i = 0; i < fullCommand.length(); ++i) {
            if (fullCommand.charAt(i) == '[') {
                ++level;
                continue;
            }
            if (fullCommand.charAt(i) != ']') continue;
            if (level == 0) {
                Skript.error("Invalid placement of [optional brackets]");
                this.getParser().deleteCurrentEvent();
                return false;
            }
            --level;
        }
        if (level > 0) {
            Skript.error("Invalid amount of [optional brackets]");
            this.getParser().deleteCurrentEvent();
            return false;
        }
        Matcher matcher = COMMAND_PATTERN.matcher(fullCommand);
        boolean matches = matcher.matches();
        if (!matches) {
            Skript.error("Invalid command structure pattern");
            return false;
        }
        String command = matcher.group(1).toLowerCase();
        ScriptCommand existingCommand = Commands.getScriptCommand(command);
        if (existingCommand != null && existingCommand.getLabel().equals(command)) {
            Script script = existingCommand.getScript();
            Skript.error("A command with the name /" + existingCommand.getName() + " is already defined" + (String)(script != null ? " in " + script.getConfig().getFileName() : ""));
            this.getParser().deleteCurrentEvent();
            return false;
        }
        String arguments = matcher.group(3) == null ? "" : matcher.group(3);
        StringBuilder pattern = new StringBuilder();
        Commands.currentArguments = new ArrayList();
        ArrayList currentArguments = Commands.currentArguments;
        matcher = ARGUMENT_PATTERN.matcher(arguments);
        int lastEnd = 0;
        int optionals = 0;
        int i = 0;
        while (matcher.find()) {
            pattern.append(Commands.escape(arguments.substring(lastEnd, matcher.start())));
            optionals += StringUtils.count(arguments, '[', lastEnd, matcher.start());
            optionals -= StringUtils.count(arguments, ']', lastEnd, matcher.start());
            lastEnd = matcher.end();
            ClassInfo<?> c = Classes.getClassInfoFromUserInput(matcher.group(2));
            NonNullPair<String, Boolean> p = Utils.getEnglishPlural(matcher.group(2));
            if (c == null) {
                c = Classes.getClassInfoFromUserInput(p.getFirst());
            }
            if (c == null) {
                Skript.error("Unknown type '" + matcher.group(2) + "'");
                this.getParser().deleteCurrentEvent();
                return false;
            }
            Parser<?> parser = c.getParser();
            if (parser == null || !parser.canParse(ParseContext.COMMAND)) {
                Skript.error("Can't use " + String.valueOf(c) + " as argument of a command");
                this.getParser().deleteCurrentEvent();
                return false;
            }
            Argument<?> arg = Argument.newInstance(matcher.group(1), c, matcher.group(3), i, p.getSecond() == false, optionals > 0);
            if (arg == null) {
                this.getParser().deleteCurrentEvent();
                return false;
            }
            currentArguments.add(arg);
            if (arg.isOptional() && optionals == 0) {
                pattern.append('[');
                ++optionals;
            }
            pattern.append("%").append(arg.isOptional() ? "-" : "").append(Utils.toEnglishPlural(c.getCodeName(), p.getSecond())).append("%");
            ++i;
        }
        pattern.append(Commands.escape(arguments.substring(lastEnd)));
        optionals += StringUtils.count(arguments, '[', lastEnd);
        optionals -= StringUtils.count(arguments, ']', lastEnd);
        for (i = 0; i < optionals; ++i) {
            pattern.append(']');
        }
        Object desc = "/" + command + " ";
        desc = (String)desc + StringUtils.replaceAll((CharSequence)pattern, DESCRIPTION_PATTERN, m1 -> {
            assert (m1 != null);
            NonNullPair<String, Boolean> p = Utils.getEnglishPlural(m1.group(1));
            String s1 = p.getFirst();
            return "<" + Classes.getClassInfo(s1).getName().toString(p.getSecond()) + ">";
        });
        desc = Commands.unescape((String)desc).trim();
        VariableString usageMessage = (VariableString)this.entryContainer.getOptional("usage", VariableString.class, false);
        String defaultUsageMessage = String.valueOf(Commands.m_correct_usage) + " " + (String)desc;
        CommandUsage usage = new CommandUsage(usageMessage, defaultUsageMessage);
        String description = (String)this.entryContainer.get("description", String.class, true);
        String prefix = (String)this.entryContainer.getOptional("prefix", String.class, false);
        String permission = (String)this.entryContainer.get("permission", String.class, true);
        VariableString permissionMessage = (VariableString)this.entryContainer.getOptional("permission message", VariableString.class, false);
        if (permissionMessage != null && permission.isEmpty()) {
            Skript.warning("command /" + command + " has a permission message set, but not a permission");
        }
        List aliases = (List)this.entryContainer.get("aliases", List.class, true);
        int executableBy = (Integer)this.entryContainer.get("executable by", Integer.class, true);
        Timespan cooldown = (Timespan)this.entryContainer.getOptional("cooldown", Timespan.class, false);
        VariableString cooldownMessage = (VariableString)this.entryContainer.getOptional("cooldown message", VariableString.class, false);
        if (cooldownMessage != null && cooldown == null) {
            Skript.warning("command /" + command + " has a cooldown message set, but not a cooldown");
        }
        if ((cooldownBypass = (String)this.entryContainer.getOptional("cooldown bypass", String.class, false)) == null) {
            cooldownBypass = "";
        } else if (cooldownBypass.isEmpty() && cooldown == null) {
            Skript.warning("command /" + command + " has a cooldown bypass set, but not a cooldown");
        }
        VariableString cooldownStorage = (VariableString)this.entryContainer.getOptional("cooldown storage", VariableString.class, false);
        if (cooldownStorage != null && cooldown == null) {
            Skript.warning("command /" + command + " has a cooldown storage set, but not a cooldown");
        }
        SectionNode node = this.entryContainer.getSource();
        if (Skript.debug() || node.debug()) {
            Skript.debug("command " + (String)desc + ":");
        }
        Commands.currentArguments = currentArguments;
        try {
            this.scriptCommand = new ScriptCommand(this.getParser().getCurrentScript(), command, pattern.toString(), currentArguments, description, prefix, usage, (List<String>)aliases, permission, permissionMessage, cooldown, cooldownMessage, cooldownBypass, cooldownStorage, executableBy, (SectionNode)this.entryContainer.get("trigger", SectionNode.class, false));
        }
        finally {
            Commands.currentArguments = null;
        }
        if (Skript.logVeryHigh() && !Skript.debug()) {
            Skript.info("Registered command " + (String)desc);
        }
        this.getParser().deleteCurrentEvent();
        Commands.registerCommand(this.scriptCommand);
        SYNC_COMMANDS.set(true);
        return true;
    }

    @Override
    public boolean postLoad() {
        this.scheduleCommandSync();
        return true;
    }

    @Override
    public void unload() {
        assert (this.scriptCommand != null);
        Commands.unregisterCommand(this.scriptCommand);
        SYNC_COMMANDS.set(true);
    }

    @Override
    public void postUnload() {
        this.scheduleCommandSync();
    }

    private void scheduleCommandSync() {
        if (SYNC_COMMANDS.get()) {
            SYNC_COMMANDS.set(false);
            if (DELAY_COMMAND_SYNCING) {
                if (Bukkit.getPluginManager().isPluginEnabled((Plugin)Skript.getInstance())) {
                    Bukkit.getScheduler().runTask((Plugin)Skript.getInstance(), this::forceCommandSync);
                }
            } else {
                this.forceCommandSync();
            }
        }
    }

    private void forceCommandSync() {
        if (CommandReloader.syncCommands(Bukkit.getServer())) {
            Skript.debug("Commands synced to clients");
        } else {
            Skript.debug("Commands changed but not synced to clients (normal on 1.12 and older)");
        }
    }

    @Override
    public Structure.Priority getPriority() {
        return PRIORITY;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "command";
    }

    static {
        Skript.registerStructure(StructCommand.class, EntryValidator.builder().addEntryData(new VariableStringEntryData("usage", null, true)).addEntry("description", "", true).addEntry("prefix", null, true).addEntry("permission", "", true).addEntryData(new VariableStringEntryData("permission message", null, true)).addEntryData(new KeyValueEntryData<List<String>>("aliases", new ArrayList(), true){
            private final Pattern pattern = Pattern.compile("\\s*,\\s*/?");

            @Override
            protected List<String> getValue(String value) {
                ArrayList<String> aliases = new ArrayList<String>(Arrays.asList(this.pattern.split(value)));
                if (((String)aliases.get(0)).startsWith("/")) {
                    aliases.set(0, ((String)aliases.get(0)).substring(1));
                } else if (((String)aliases.get(0)).isEmpty()) {
                    aliases = new ArrayList(0);
                }
                return aliases;
            }
        }).addEntryData(new KeyValueEntryData<Integer>("executable by", Integer.valueOf(3), true){
            private final Pattern pattern = Pattern.compile("\\s*,\\s*|\\s+(and|or)\\s+");

            @Override
            @Nullable
            protected Integer getValue(String value) {
                int executableBy = 0;
                for (String b : this.pattern.split(value)) {
                    if (b.equalsIgnoreCase("console") || b.equalsIgnoreCase("the console")) {
                        executableBy |= 2;
                        continue;
                    }
                    if (b.equalsIgnoreCase("players") || b.equalsIgnoreCase("player")) {
                        executableBy |= 1;
                        continue;
                    }
                    return null;
                }
                return executableBy;
            }
        }).addEntryData(new LiteralEntryData<Timespan>("cooldown", null, true, Timespan.class)).addEntryData(new VariableStringEntryData("cooldown message", null, true)).addEntry("cooldown bypass", null, true).addEntryData(new VariableStringEntryData("cooldown storage", null, true, StringMode.VARIABLE_NAME)).addSection("trigger", false).unexpectedEntryMessage(key -> "Unexpected entry '" + key + "'. Check that it's spelled correctly, and ensure that you have put all code into a trigger.").build(), "command <.+>");
    }
}

