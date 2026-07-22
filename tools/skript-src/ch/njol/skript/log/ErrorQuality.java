/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.log;

public enum ErrorQuality {
    NONE,
    NOT_AN_EXPRESSION,
    SEMANTIC_ERROR;


    public int quality() {
        return this.ordinal();
    }

    public static ErrorQuality get(int quality) {
        return ErrorQuality.values()[quality];
    }
}

