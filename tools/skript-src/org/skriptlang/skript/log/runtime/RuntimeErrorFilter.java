/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.log.runtime;

import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.log.runtime.Frame;
import org.skriptlang.skript.log.runtime.RuntimeError;

public class RuntimeErrorFilter {
    public static final RuntimeErrorFilter NO_FILTER = new RuntimeErrorFilter(new Frame.FrameLimit(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), new Frame.FrameLimit(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)){

        @Override
        public boolean test(@NotNull RuntimeError error) {
            return true;
        }
    };
    private Frame errorFrame;
    private Frame warningFrame;

    public RuntimeErrorFilter(Frame.FrameLimit errorFrameLimits, Frame.FrameLimit warningFrameLimits) {
        this.errorFrame = new Frame(errorFrameLimits);
        this.warningFrame = new Frame(warningFrameLimits);
    }

    public boolean test(@NotNull RuntimeError error) {
        return error.level() == Level.SEVERE && this.errorFrame.add(error) || error.level() == Level.WARNING && this.warningFrame.add(error);
    }

    public void setErrorFrameLimits(Frame.FrameLimit limits) {
        this.errorFrame = new Frame(limits);
    }

    public void setWarningFrameLimits(Frame.FrameLimit limits) {
        this.warningFrame = new Frame(limits);
    }

    public Frame getErrorFrame() {
        return this.errorFrame;
    }

    public Frame getWarningFrame() {
        return this.warningFrame;
    }
}

