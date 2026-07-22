/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.lang.comparator;

public enum Relation {
    EQUAL("equal to"),
    NOT_EQUAL("not equal to"),
    GREATER("greater than"),
    GREATER_OR_EQUAL("greater than or equal to"),
    SMALLER("smaller than"),
    SMALLER_OR_EQUAL("smaller than or equal to");

    private final String toString;

    private Relation(String toString) {
        this.toString = toString;
    }

    public static Relation get(boolean b) {
        return b ? EQUAL : NOT_EQUAL;
    }

    public static Relation get(int i) {
        return i == 0 ? EQUAL : (i > 0 ? GREATER : SMALLER);
    }

    public static Relation get(double d) {
        return d == 0.0 ? EQUAL : (d > 0.0 ? GREATER : SMALLER);
    }

    public boolean isImpliedBy(Relation other) {
        if (other == this) {
            return true;
        }
        switch (this.ordinal()) {
            case 0: 
            case 2: 
            case 4: {
                return false;
            }
            case 1: {
                return other == SMALLER || other == GREATER;
            }
            case 3: {
                return other == GREATER || other == EQUAL;
            }
            case 5: {
                return other == SMALLER || other == EQUAL;
            }
        }
        throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)this));
    }

    public boolean isImpliedBy(Relation ... others) {
        for (Relation other : others) {
            if (!this.isImpliedBy(other)) continue;
            return true;
        }
        return false;
    }

    public String toString() {
        return this.toString;
    }

    public Relation getInverse() {
        switch (this.ordinal()) {
            case 0: {
                return NOT_EQUAL;
            }
            case 1: {
                return EQUAL;
            }
            case 2: {
                return SMALLER_OR_EQUAL;
            }
            case 3: {
                return SMALLER;
            }
            case 4: {
                return GREATER_OR_EQUAL;
            }
            case 5: {
                return GREATER;
            }
        }
        throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)this));
    }

    public Relation getSwitched() {
        switch (this.ordinal()) {
            case 0: {
                return EQUAL;
            }
            case 1: {
                return NOT_EQUAL;
            }
            case 2: {
                return SMALLER;
            }
            case 3: {
                return SMALLER_OR_EQUAL;
            }
            case 4: {
                return GREATER;
            }
            case 5: {
                return GREATER_OR_EQUAL;
            }
        }
        throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)this));
    }

    public int getRelation() {
        switch (this.ordinal()) {
            case 0: 
            case 1: {
                return 0;
            }
            case 2: 
            case 3: {
                return 1;
            }
            case 4: 
            case 5: {
                return -1;
            }
        }
        throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)this));
    }
}

