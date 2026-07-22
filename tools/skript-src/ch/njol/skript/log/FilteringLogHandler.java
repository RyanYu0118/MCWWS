/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.log;

import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.SkriptLogger;
import java.util.logging.Level;

public class FilteringLogHandler
extends LogHandler {
    private final int minimum;

    public FilteringLogHandler(Level minimum) {
        this.minimum = minimum.intValue();
    }

    @Override
    public LogHandler.LogResult log(LogEntry entry) {
        return entry.level.intValue() >= this.minimum ? LogHandler.LogResult.LOG : LogHandler.LogResult.DO_NOT_LOG;
    }

    @Override
    public FilteringLogHandler start() {
        SkriptLogger.startLogHandler(this);
        return this;
    }
}

