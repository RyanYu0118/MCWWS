/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnmodifiableView
 */
package org.skriptlang.skript.log.runtime;

import ch.njol.skript.Skript;
import ch.njol.skript.log.SkriptLogger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.skriptlang.skript.log.runtime.Frame;
import org.skriptlang.skript.log.runtime.RuntimeError;
import org.skriptlang.skript.log.runtime.RuntimeErrorConsumer;
import org.skriptlang.skript.log.runtime.RuntimeErrorFilter;
import org.skriptlang.skript.log.runtime.RuntimeErrorManager;

public class RuntimeErrorCatcher
implements RuntimeErrorConsumer,
AutoCloseable {
    private List<RuntimeErrorConsumer> storedConsumers = new ArrayList<RuntimeErrorConsumer>();
    private final List<RuntimeError> cachedErrors = new ArrayList<RuntimeError>();
    private static final int ERROR_LIMIT = 1000;
    private boolean stopped = false;

    private RuntimeErrorManager getManager() {
        return Skript.getRuntimeErrorManager();
    }

    @Override
    @Nullable
    public RuntimeErrorFilter getFilter() {
        return RuntimeErrorFilter.NO_FILTER;
    }

    public RuntimeErrorCatcher start() {
        this.stopped = false;
        this.storedConsumers = this.getManager().removeAllConsumers();
        this.getManager().addConsumer(this);
        return this;
    }

    public void stop() {
        if (this.stopped) {
            return;
        }
        this.stopped = true;
        if (!this.getManager().removeConsumer(this)) {
            SkriptLogger.LOGGER.severe("[Skript] A 'RuntimeErrorCatcher' was stopped incorrectly.");
            return;
        }
        this.getManager().addConsumers((RuntimeErrorConsumer[])this.storedConsumers.toArray(RuntimeErrorConsumer[]::new));
    }

    public @UnmodifiableView List<RuntimeError> getCachedErrors() {
        return Collections.unmodifiableList(this.cachedErrors);
    }

    public RuntimeErrorCatcher clearCachedErrors() {
        this.cachedErrors.clear();
        return this;
    }

    @Override
    public void printError(RuntimeError error) {
        if (this.cachedErrors.size() < 1000) {
            this.cachedErrors.add(error);
        }
    }

    @Override
    public void printFrameOutput(Frame.FrameOutput output, Level level) {
    }

    @Override
    public void close() {
        this.clearCachedErrors().stop();
    }
}

