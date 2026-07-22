/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.ConsoleCommandSender
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.log;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.log.HandlerList;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.Verbosity;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentParser;

public abstract class SkriptLogger {
    public static final Level SEVERE = Level.SEVERE;
    private static Verbosity verbosity = Verbosity.NORMAL;
    private static boolean debug;
    public static final Level DEBUG;
    public static final Logger LOGGER;

    private static HandlerList getHandlers() {
        return ParserInstance.get().getHandlers();
    }

    public static RetainingLogHandler startRetainingLog() {
        return new RetainingLogHandler().start();
    }

    public static ParseLogHandler startParseLogHandler() {
        return new ParseLogHandler().start();
    }

    public static <T extends LogHandler> T startLogHandler(T h) {
        SkriptLogger.getHandlers().add(h);
        return h;
    }

    static void removeHandler(LogHandler h) {
        HandlerList handlers = SkriptLogger.getHandlers();
        if (!handlers.contains(h)) {
            return;
        }
        if (!h.equals(handlers.remove())) {
            int i = 1;
            while (!h.equals(handlers.remove())) {
                ++i;
            }
            LOGGER.severe("[Skript] " + i + " log handler" + (i == 1 ? " was" : "s were") + " not stopped properly! (at " + String.valueOf(SkriptLogger.getCaller()) + ") [if you're a server admin and you see this message please file a bug report at https://github.com/SkriptLang/skript/issues if there is not already one]");
        }
    }

    static boolean isStopped(LogHandler h) {
        return !SkriptLogger.getHandlers().contains(h);
    }

    @Nullable
    static StackTraceElement getCaller() {
        for (StackTraceElement e : new Exception().getStackTrace()) {
            if (e.getClassName().startsWith(SkriptLogger.class.getPackage().getName())) continue;
            return e;
        }
        return null;
    }

    public static void setVerbosity(Verbosity v) {
        verbosity = v;
        debug = v.compareTo(Verbosity.DEBUG) >= 0;
    }

    public static boolean debug() {
        return debug;
    }

    public static void setNode(@Nullable Node node) {
        ParserInstance.get().setNode(node);
    }

    @Nullable
    public static Node getNode() {
        return ParserInstance.get().getNode();
    }

    public static void log(Level level, String message) {
        SkriptLogger.log(new LogEntry(level, message, SkriptLogger.getNode()));
    }

    public static void log(@Nullable LogEntry entry) {
        if (entry == null) {
            return;
        }
        if (Skript.testing() && SkriptLogger.getNode() != null && SkriptLogger.getNode().debug()) {
            System.out.print("---> " + String.valueOf(entry.level) + "/" + String.valueOf((Object)ErrorQuality.get(entry.quality)) + ": " + String.valueOf(entry) + " ::" + LogEntry.findCaller());
        }
        block5: for (LogHandler h : SkriptLogger.getHandlers()) {
            LogHandler.LogResult r = h.log(entry);
            switch (r) {
                case CACHED: {
                    return;
                }
                case DO_NOT_LOG: {
                    entry.discarded("denied by " + String.valueOf(h));
                    return;
                }
                case LOG: {
                    continue block5;
                }
            }
        }
        entry.logged();
        SkriptLogger.sendFormatted((CommandSender)Bukkit.getConsoleSender(), Skript.getSkriptPrefix() + entry.toFormattedString());
    }

    public static void logAll(Collection<LogEntry> entries) {
        entries.forEach(SkriptLogger::log);
    }

    public static void logTracked(Level level, String message, ErrorQuality quality) {
        SkriptLogger.log(new LogEntry(level, quality.quality(), message, SkriptLogger.getNode(), true));
    }

    public static boolean log(Verbosity minVerb) {
        return minVerb.compareTo(verbosity) <= 0;
    }

    public static void sendFormatted(CommandSender commandSender, String message) {
        if (commandSender instanceof ConsoleCommandSender) {
            for (String s : message.split("\n")) {
                commandSender.sendMessage(TextComponentParser.instance().parse(s));
            }
        } else {
            commandSender.sendMessage(TextComponentParser.instance().parse(message));
        }
    }

    static {
        DEBUG = Level.INFO;
        LOGGER = Bukkit.getServer() != null ? Bukkit.getLogger() : Logger.getLogger("global");
    }
}

