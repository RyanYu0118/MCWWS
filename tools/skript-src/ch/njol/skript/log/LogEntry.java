/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.log;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.log.SkriptLogger;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentParser;

public class LogEntry {
    public final Level level;
    public final int quality;
    public final String message;
    @Nullable
    public final Node node;
    private final String from;
    private final boolean tracked;
    private static final String CONFIG_NODE = "skript command.reload";
    private static final ArgsMessage WARNING_LINE_INFO = new ArgsMessage("skript command.reload.warning line info");
    private static final ArgsMessage ERROR_LINE_INFO = new ArgsMessage("skript command.reload.error line info");
    private static final ArgsMessage WARNING_DETAILS = new ArgsMessage("skript command.reload.warning details");
    private static final ArgsMessage ERROR_DETAILS = new ArgsMessage("skript command.reload.error details");
    private static final ArgsMessage OTHER_DETAILS = new ArgsMessage("skript command.reload.other details");
    private static final ArgsMessage LINE_DETAILS = new ArgsMessage("skript command.reload.line details");
    private static final String skriptLogPackageName = SkriptLogger.class.getPackage().getName();
    private boolean used;

    public LogEntry(Level level, String message) {
        this(level, ErrorQuality.SEMANTIC_ERROR.quality(), message, SkriptLogger.getNode());
    }

    public LogEntry(Level level, int quality, String message) {
        this(level, quality, message, SkriptLogger.getNode());
    }

    public LogEntry(Level level, ErrorQuality quality, String message) {
        this(level, quality.quality(), message, SkriptLogger.getNode());
    }

    public LogEntry(Level level, String message, @Nullable Node node) {
        this(level, ErrorQuality.SEMANTIC_ERROR.quality(), message, node);
    }

    public LogEntry(Level level, ErrorQuality quality, String message, Node node) {
        this(level, quality.quality(), message, node);
    }

    public LogEntry(Level level, int quality, String message, @Nullable Node node) {
        this(level, quality, message, node, false);
    }

    public LogEntry(Level level, int quality, String message, @Nullable Node node, boolean tracked) {
        this.level = level;
        this.quality = quality;
        this.message = message;
        this.node = node;
        this.tracked = tracked;
        this.from = tracked || Skript.debug() ? LogEntry.findCaller() : "";
    }

    static String findCaller() {
        StackTraceElement[] es = new Exception().getStackTrace();
        for (int i = 0; i < es.length; ++i) {
            if (!es[i].getClassName().startsWith(skriptLogPackageName)) continue;
            ++i;
            while (i < es.length - 1 && (es[i].getClassName().startsWith(skriptLogPackageName) || es[i].getClassName().equals(Skript.class.getName()))) {
                ++i;
            }
            if (i >= es.length) {
                i = es.length - 1;
            }
            return " (from " + String.valueOf(es[i]) + ")";
        }
        return " (from an unknown source)";
    }

    public Level getLevel() {
        return this.level;
    }

    public int getQuality() {
        return this.quality;
    }

    public String getMessage() {
        return this.message;
    }

    void discarded(String info) {
        this.used = true;
        if (this.tracked) {
            SkriptLogger.LOGGER.warning(" # LogEntry '" + this.message + "'" + this.from + " discarded" + LogEntry.findCaller() + "; " + String.valueOf(new Exception().getStackTrace()[1]) + "; " + info);
        }
    }

    void logged() {
        this.used = true;
        if (this.tracked) {
            SkriptLogger.LOGGER.warning(" # LogEntry '" + this.message + "'" + this.from + " logged" + LogEntry.findCaller());
        }
    }

    public String toString() {
        if (this.node == null || this.level.intValue() < Level.WARNING.intValue()) {
            return this.message;
        }
        Config c = this.node.getConfig();
        return this.message + this.from + " (" + c.getFileName() + ", line " + this.node.getLine() + ": " + this.node.save().trim() + "')";
    }

    public String toFormattedString() {
        ArgsMessage details;
        if (this.level.intValue() < Level.WARNING.intValue()) {
            return this.message;
        }
        ArgsMessage lineInfo = WARNING_LINE_INFO;
        if (this.level.intValue() == Level.WARNING.intValue()) {
            details = WARNING_DETAILS;
        } else if (this.level.intValue() == Level.SEVERE.intValue()) {
            details = ERROR_DETAILS;
            lineInfo = ERROR_LINE_INFO;
        } else {
            details = OTHER_DETAILS;
        }
        String lineInfoMsg = this.replaceNewline(lineInfo.getValue() == null ? lineInfo.key : lineInfo.getValue());
        String detailsMsg = this.replaceNewline(details.getValue() == null ? details.key : details.getValue());
        String lineDetailsMsg = this.replaceNewline(LINE_DETAILS.getValue() == null ? LogEntry.LINE_DETAILS.key : LINE_DETAILS.getValue());
        if (this.node == null) {
            return String.format(detailsMsg.stripLeading(), this.message);
        }
        Config c = this.node.getConfig();
        Object from = this.from;
        if (!((String)from).isEmpty()) {
            from = String.valueOf(ChatColor.GRAY) + "   " + (String)from + "\n";
        }
        return String.format(lineInfoMsg, this.node.getLine(), c.getFileName()) + String.format(detailsMsg, TextComponentParser.instance().escape(this.message.replaceAll("\u00a7", "&"))) + (String)from + String.format(lineDetailsMsg, TextComponentParser.instance().escape(this.node.save().trim().replaceAll("\u00a7", "&")));
    }

    private String replaceNewline(String s) {
        return s.replaceAll("\\\\n", "\n");
    }
}

