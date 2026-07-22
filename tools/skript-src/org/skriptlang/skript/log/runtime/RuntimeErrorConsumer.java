/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.log.runtime;

import java.util.logging.Level;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.Frame;
import org.skriptlang.skript.log.runtime.RuntimeError;
import org.skriptlang.skript.log.runtime.RuntimeErrorFilter;
import org.skriptlang.skript.log.runtime.RuntimeErrorManager;

public interface RuntimeErrorConsumer {
    public void printError(RuntimeError var1);

    @Nullable
    default public RuntimeErrorFilter getFilter() {
        return RuntimeErrorManager.standardFilter;
    }

    public void printFrameOutput(Frame.FrameOutput var1, Level var2);
}

