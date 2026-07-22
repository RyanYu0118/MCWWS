/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.log;

import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.SkriptLogger;

public class BlockingLogHandler
extends LogHandler {
    @Override
    public LogHandler.LogResult log(LogEntry entry) {
        return LogHandler.LogResult.DO_NOT_LOG;
    }

    @Override
    public BlockingLogHandler start() {
        SkriptLogger.startLogHandler(this);
        return this;
    }
}

