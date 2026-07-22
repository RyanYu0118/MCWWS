/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.log.runtime;

import ch.njol.skript.Skript;
import java.util.logging.Level;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.log.runtime.ErrorSource;
import org.skriptlang.skript.log.runtime.RuntimeError;
import org.skriptlang.skript.log.runtime.RuntimeErrorManager;

public interface RuntimeErrorProducer {
    @Contract(value=" -> new")
    @NotNull
    public ErrorSource getErrorSource();

    default public void error(String message) {
        this.getRuntimeErrorManager().error(new RuntimeError(Level.SEVERE, this.getErrorSource(), message, null));
    }

    default public void error(String message, String highlight) {
        this.getRuntimeErrorManager().error(new RuntimeError(Level.SEVERE, this.getErrorSource(), message, highlight));
    }

    default public void warning(String message) {
        this.getRuntimeErrorManager().error(new RuntimeError(Level.WARNING, this.getErrorSource(), message, null));
    }

    default public void warning(String message, String highlight) {
        this.getRuntimeErrorManager().error(new RuntimeError(Level.WARNING, this.getErrorSource(), message, highlight));
    }

    default public RuntimeErrorManager getRuntimeErrorManager() {
        return Skript.getRuntimeErrorManager();
    }
}

