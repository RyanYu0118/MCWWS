/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.log;

import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.SkriptLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class ParseLogHandler
extends LogHandler {
    @Nullable
    private LogEntry error = null;
    private final List<LogEntry> log = new ArrayList<LogEntry>();
    boolean printedErrorOrLog = false;

    @ApiStatus.Internal
    @Contract(value="-> new")
    public ParseLogHandler backup() {
        ParseLogHandler copy = new ParseLogHandler();
        copy.error = this.error;
        copy.log.addAll(this.log);
        return copy;
    }

    @ApiStatus.Internal
    public void restore(ParseLogHandler parseLogHandler) {
        this.error = parseLogHandler.error;
        this.log.clear();
        this.log.addAll(parseLogHandler.log);
    }

    @Override
    public LogHandler.LogResult log(LogEntry entry) {
        if (entry.getLevel().intValue() >= Level.SEVERE.intValue() && (this.error == null || entry.getQuality() > this.error.getQuality())) {
            this.error = entry;
        }
        this.log.add(entry);
        return LogHandler.LogResult.CACHED;
    }

    @Override
    public ParseLogHandler start() {
        SkriptLogger.startLogHandler(this);
        return this;
    }

    public void error(String error, ErrorQuality quality) {
        this.log(new LogEntry(SkriptLogger.SEVERE, quality, error));
    }

    public void clear() {
        for (LogEntry e : this.log) {
            e.discarded("cleared");
        }
        this.log.clear();
    }

    public void clearError() {
        if (this.error != null) {
            this.error.discarded("cleared");
        }
        this.error = null;
    }

    public void printLog() {
        this.printLog(true);
    }

    public void printLog(boolean includeErrors) {
        this.printedErrorOrLog = true;
        this.stop();
        for (LogEntry logEntry : this.log) {
            if (!includeErrors && logEntry.getLevel().intValue() >= Level.SEVERE.intValue()) continue;
            SkriptLogger.log(logEntry);
        }
        if (this.error != null) {
            this.error.discarded("not printed");
        }
    }

    public void printError() {
        this.printError(null);
    }

    public void printError(@Nullable String def) {
        this.printedErrorOrLog = true;
        this.stop();
        LogEntry error = this.error;
        if (error != null) {
            SkriptLogger.log(error);
        } else if (def != null) {
            SkriptLogger.log(new LogEntry(SkriptLogger.SEVERE, ErrorQuality.SEMANTIC_ERROR, def));
        }
        for (LogEntry e : this.log) {
            e.discarded("not printed");
        }
    }

    public void printError(String def, ErrorQuality quality) {
        this.printedErrorOrLog = true;
        this.stop();
        LogEntry error = this.error;
        if (error != null && error.quality >= quality.quality()) {
            SkriptLogger.log(error);
        } else {
            SkriptLogger.log(new LogEntry(SkriptLogger.SEVERE, quality, def));
        }
        for (LogEntry e : this.log) {
            e.discarded("not printed");
        }
    }

    public int getNumErrors() {
        return this.error == null ? 0 : 1;
    }

    public boolean hasError() {
        return this.error != null;
    }

    @Nullable
    public LogEntry getError() {
        return this.error;
    }
}

