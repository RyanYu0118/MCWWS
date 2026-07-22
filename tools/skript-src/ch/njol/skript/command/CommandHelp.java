/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.command;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Message;
import ch.njol.skript.util.SkriptColor;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class CommandHelp {
    private static final String DEFAULTENTRY = "description";
    private static final ArgsMessage m_invalid_argument = new ArgsMessage("commands.invalid argument");
    private static final Message m_usage = new Message("skript command.usage");
    private final String actualCommand;
    private final String actualNode;
    private final String argsColor;
    private String command;
    private String langNode;
    private boolean revalidate = true;
    @Nullable
    private Message description = null;
    private final Map<String, Object> arguments = new LinkedHashMap<String, Object>();
    @Nullable
    private ArgumentHolder wildcardArg = null;

    public CommandHelp(String command, SkriptColor argsColor, String langNode) {
        this(command, argsColor.getFormattedChat(), langNode);
    }

    public CommandHelp(String command, SkriptColor argsColor) {
        this(command, argsColor.getFormattedChat(), command);
    }

    private CommandHelp(String command, String argsColor, String node) {
        this.actualCommand = this.command = command;
        this.actualNode = this.langNode = node;
        this.argsColor = argsColor;
    }

    public CommandHelp add(String argument) {
        ArgumentHolder holder = new ArgumentHolder((String)argument);
        if (((String)argument).startsWith("<") && ((String)argument).endsWith(">")) {
            argument = String.valueOf(ChatColor.GRAY) + "<" + this.argsColor + ((String)argument).substring(1, ((String)argument).length() - 1) + String.valueOf(ChatColor.GRAY) + ">";
            this.wildcardArg = holder;
        }
        this.arguments.put((String)argument, holder);
        return this;
    }

    public CommandHelp add(CommandHelp help) {
        this.arguments.put(help.command, help);
        help.onAdd(this);
        return this;
    }

    protected void onAdd(CommandHelp parent) {
        this.langNode = parent.langNode + "." + this.actualNode;
        this.command = parent.command + " " + parent.argsColor + this.actualCommand;
        this.revalidate = true;
        for (Map.Entry<String, Object> entry : this.arguments.entrySet()) {
            if (entry.getValue() instanceof CommandHelp) {
                ((CommandHelp)entry.getValue()).onAdd(this);
                continue;
            }
            ((ArgumentHolder)entry.getValue()).revalidate = true;
        }
    }

    public boolean test(CommandSender sender, String[] args) {
        return this.test(sender, args, 0);
    }

    private boolean test(CommandSender sender, String[] args, int index) {
        if (index >= args.length) {
            this.showHelp(sender);
            return false;
        }
        Object help = this.arguments.get(args[index].toLowerCase(Locale.ENGLISH));
        if (help == null && this.wildcardArg == null) {
            this.showHelp(sender, m_invalid_argument.toString(this.argsColor + args[index]));
            return false;
        }
        return !(help instanceof CommandHelp) || ((CommandHelp)help).test(sender, args, index + 1);
    }

    public void showHelp(CommandSender sender) {
        this.showHelp(sender, m_usage.toString());
    }

    private void showHelp(CommandSender sender, String pre) {
        Skript.message(sender, pre + " " + this.command + " " + this.argsColor + "...");
        for (Map.Entry<String, Object> entry : this.arguments.entrySet()) {
            Skript.message(sender, "  " + this.argsColor + entry.getKey() + " " + String.valueOf(ChatColor.GRAY) + "-" + String.valueOf(ChatColor.RESET) + " " + String.valueOf(entry.getValue()));
        }
    }

    public String toString() {
        if (this.revalidate) {
            this.description = new Message(this.langNode + ".description");
            this.revalidate = false;
        }
        return String.valueOf(this.description);
    }

    private class ArgumentHolder {
        private final String argument;
        private boolean revalidate = true;
        @Nullable
        private Message description = null;

        private ArgumentHolder(String argument) {
            this.argument = argument;
        }

        public String toString() {
            if (this.revalidate) {
                this.description = new Message(CommandHelp.this.langNode + "." + this.argument);
                this.revalidate = false;
            }
            return String.valueOf(this.description);
        }
    }
}

