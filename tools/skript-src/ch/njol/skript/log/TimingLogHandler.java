/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.log;

import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.SkriptLogger;

public class TimingLogHandler
extends LogHandler {
    private final long start = System.currentTimeMillis();

    @Override
    public LogHandler.LogResult log(LogEntry entry) {
        return LogHandler.LogResult.LOG;
    }

    @Override
    public TimingLogHandler start() {
        return SkriptLogger.startLogHandler(this);
    }

    public long getStart() {
        return this.start;
    }

    public long getTimeTaken() {
        return System.currentTimeMillis() - this.start;
    }
}

