/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.log;

import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.OpenCloseable;
import java.io.Closeable;

public abstract class LogHandler
implements Closeable,
OpenCloseable {
    public abstract LogResult log(LogEntry var1);

    protected void onStop() {
    }

    public final void stop() {
        SkriptLogger.removeHandler(this);
        this.onStop();
    }

    public boolean isStopped() {
        return SkriptLogger.isStopped(this);
    }

    public LogHandler start() {
        SkriptLogger.startLogHandler(this);
        return this;
    }

    @Override
    public void open() {
        this.start();
    }

    @Override
    public void close() {
        this.stop();
    }

    public static enum LogResult {
        LOG,
        CACHED,
        DO_NOT_LOG;

    }
}

