/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.log;

import ch.njol.skript.Skript;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.SkriptLogger;
import java.util.logging.Level;
import org.jetbrains.annotations.Nullable;

public class ErrorDescLogHandler
extends LogHandler {
    @Nullable
    private final String before;
    @Nullable
    private final String after;
    @Nullable
    private final String success;
    private boolean hadError = false;

    public ErrorDescLogHandler() {
        this(null, null, null);
    }

    public ErrorDescLogHandler(@Nullable String before, @Nullable String after, @Nullable String success) {
        this.before = before;
        this.after = after;
        this.success = success;
    }

    @Override
    public LogHandler.LogResult log(LogEntry entry) {
        if (!this.hadError && entry.getLevel() == Level.SEVERE) {
            this.hadError = true;
            this.beforeErrors();
        }
        return LogHandler.LogResult.LOG;
    }

    @Override
    public ErrorDescLogHandler start() {
        SkriptLogger.startLogHandler(this);
        return this;
    }

    protected void beforeErrors() {
        if (this.before != null) {
            Skript.error(this.before);
        }
    }

    protected void afterErrors() {
        if (this.after != null) {
            Skript.error(this.after);
        }
    }

    protected void onSuccess() {
        if (this.success != null) {
            Skript.info(this.success);
        }
    }

    @Override
    protected void onStop() {
        if (!this.hadError) {
            this.onSuccess();
        } else {
            this.afterErrors();
        }
    }
}

