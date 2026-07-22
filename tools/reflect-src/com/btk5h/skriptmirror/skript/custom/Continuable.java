/*
 * Decompiled with CFR 0.152.
 */
package com.btk5h.skriptmirror.skript.custom;

public interface Continuable {
    default public void markContinue() {
        this.setContinue(true);
    }

    public void setContinue(boolean var1);
}

