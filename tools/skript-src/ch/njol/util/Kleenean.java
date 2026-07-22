/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.util;

import java.util.Locale;

public enum Kleenean {
    FALSE,
    UNKNOWN,
    TRUE;


    public final String toString() {
        return this.name().toLowerCase(Locale.ENGLISH);
    }

    public final Kleenean is(Kleenean other) {
        if (other == UNKNOWN || this == UNKNOWN) {
            return UNKNOWN;
        }
        if (other == this) {
            return TRUE;
        }
        return FALSE;
    }

    public final Kleenean and(Kleenean other) {
        if (this == FALSE || other == FALSE) {
            return FALSE;
        }
        if (this == TRUE && other == TRUE) {
            return TRUE;
        }
        return UNKNOWN;
    }

    public final Kleenean or(Kleenean other) {
        if (this == TRUE || other == TRUE) {
            return TRUE;
        }
        if (this == FALSE && other == FALSE) {
            return FALSE;
        }
        return UNKNOWN;
    }

    public final Kleenean not() {
        if (this == TRUE) {
            return FALSE;
        }
        if (this == FALSE) {
            return TRUE;
        }
        return UNKNOWN;
    }

    public final Kleenean implies(Kleenean other) {
        if (this == FALSE || other == TRUE) {
            return TRUE;
        }
        if (this == TRUE && other == FALSE) {
            return FALSE;
        }
        return UNKNOWN;
    }

    public final boolean isTrue() {
        return this == TRUE;
    }

    public final boolean isUnknown() {
        return this == UNKNOWN;
    }

    public final boolean isFalse() {
        return this == FALSE;
    }

    public static Kleenean get(boolean b) {
        return b ? TRUE : FALSE;
    }

    public static Kleenean get(int i) {
        return i > 0 ? TRUE : (i < 0 ? FALSE : UNKNOWN);
    }

    public static Kleenean get(double d) {
        return d > 0.0 ? TRUE : (d < 0.0 ? FALSE : UNKNOWN);
    }
}

