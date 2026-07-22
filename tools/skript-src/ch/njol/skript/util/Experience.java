/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.yggdrasil.YggdrasilSerializable;
import org.jetbrains.annotations.Nullable;

public class Experience
implements YggdrasilSerializable {
    private final int xp;

    public Experience() {
        this.xp = -1;
    }

    public Experience(int xp) {
        this.xp = xp;
    }

    public int getXP() {
        return this.xp == -1 ? 1 : this.xp;
    }

    public int getInternalXP() {
        return this.xp;
    }

    public String toString() {
        return this.xp == -1 ? "xp" : this.xp + " xp";
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + this.xp;
        return result;
    }

    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Experience)) {
            return false;
        }
        Experience other = (Experience)obj;
        return this.xp == other.xp;
    }
}

