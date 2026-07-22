/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.lang.experiment;

public enum LifeCycle {
    STABLE(false),
    EXPERIMENTAL(false),
    DEPRECATED(true),
    MAINSTREAM(true),
    UNKNOWN(true);

    private final boolean warn;

    private LifeCycle(boolean warn) {
        this.warn = warn;
    }

    public boolean warn() {
        return this.warn;
    }
}

