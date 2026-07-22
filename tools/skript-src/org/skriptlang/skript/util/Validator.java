/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.util;

import org.skriptlang.skript.util.Validated;

class Validator
implements Validated {
    private volatile boolean valid = true;

    Validator() {
    }

    @Override
    public synchronized void invalidate() {
        this.valid = false;
    }

    @Override
    public boolean valid() {
        return this.valid;
    }
}

