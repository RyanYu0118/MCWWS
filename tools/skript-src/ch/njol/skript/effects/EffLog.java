/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.ExceptionUtils;
import ch.njol.util.Kleenean;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;

@Name(value="Log")
@Description(value={"Writes text into a .log file. Skript will write these files to /plugins/Skript/logs.", "NB: Using 'server.log' as the log file will write to the default server log. Omitting the log file altogether will log the message as '[Skript] [&lt;script&gt;.sk] &lt;message&gt;' in the server log."})
@Example.Examples(value={@Example(value="on join:\n\tlog \"%player% has just joined the server!\"\n"), @Example(value="on world change:\n\tlog \"Someone just went to %event-world%!\" to file \"worldlog/worlds.log\"\n"), @Example(value="on command:\n\tlog \"%player% just executed %full command%!\" to file \"server/commands.log\" with a severity of warning\n")})
@Since(value={"2.0, 2.9.0 (severities)"})
public class EffLog
extends Effect {
    private static final File logsFolder;
    static final HashMap<String, PrintWriter> writers;
    private Expression<String> messages;
    @Nullable
    private Expression<String> files;
    private Level logLevel = Level.INFO;

    private static String getLogPrefix(Level logLevel) {
        String timestamp = SkriptConfig.formatDate(System.currentTimeMillis());
        if (logLevel == Level.INFO) {
            return "[" + timestamp + "]";
        }
        return "[" + timestamp + " " + String.valueOf(logLevel) + "]";
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.messages = exprs[0];
        this.files = exprs[1];
        if (parser.mark == 1) {
            this.logLevel = Level.WARNING;
        } else if (parser.mark == 2) {
            this.logLevel = Level.SEVERE;
        }
        return true;
    }

    /*
     * WARNING - void declaration
     */
    @Override
    protected void execute(Event event) {
        for (String message : this.messages.getArray(event)) {
            Script script;
            if (this.files != null) {
                for (String string : this.files.getArray(event)) {
                    void var9_11;
                    String string2 = string.toLowerCase(Locale.ENGLISH);
                    if (!string2.endsWith(".log")) {
                        String string3 = string2 + ".log";
                    }
                    if (var9_11.equals("server.log")) {
                        SkriptLogger.LOGGER.log(this.logLevel, message);
                        continue;
                    }
                    PrintWriter logWriter = writers.get(var9_11);
                    if (logWriter == null) {
                        File logFolder = new File(logsFolder, (String)var9_11);
                        try {
                            logFolder.getParentFile().mkdirs();
                            logWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFolder, true)));
                            writers.put((String)var9_11, logWriter);
                        }
                        catch (IOException ex) {
                            Skript.error("Cannot write to log file '" + (String)var9_11 + "' (" + logFolder.getPath() + "): " + ExceptionUtils.toString(ex));
                            return;
                        }
                    }
                    logWriter.println(EffLog.getLogPrefix(this.logLevel) + " " + message);
                    logWriter.flush();
                }
                continue;
            }
            Trigger t = this.getTrigger();
            String scriptName = "---";
            if (t != null && (script = t.getScript()) != null) {
                scriptName = script.getConfig().getFileName();
            }
            SkriptLogger.LOGGER.log(this.logLevel, "[" + scriptName + "] " + message);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "log " + this.messages.toString(event, debug) + (String)(this.files != null ? " to " + this.files.toString(event, debug) : "") + (String)(this.logLevel != Level.INFO ? "with severity " + this.logLevel.toString().toLowerCase(Locale.ENGLISH) : "");
    }

    static {
        Skript.registerEffect(EffLog.class, "log %strings% [(to|in) [file[s]] %-strings%] [with [the|a] severity [of] (1:warning|2:severe)]");
        logsFolder = new File(Skript.getInstance().getDataFolder(), "logs");
        writers = new HashMap();
        Skript.closeOnDisable(() -> {
            for (PrintWriter pw : writers.values()) {
                pw.close();
            }
        });
    }
}

