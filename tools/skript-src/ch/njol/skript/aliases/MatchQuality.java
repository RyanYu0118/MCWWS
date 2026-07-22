/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.aliases;

public enum MatchQuality {
    EXACT,
    SAME_ITEM,
    SAME_MATERIAL,
    DIFFERENT;


    public boolean isBetter(MatchQuality another) {
        return this.ordinal() < another.ordinal();
    }

    public boolean isAtLeast(MatchQuality another) {
        return this.ordinal() <= another.ordinal();
    }
}

