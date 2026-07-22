/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.yggdrasil.YggdrasilSerializable;
import org.jetbrains.annotations.Nullable;

public class GameruleValue<T>
implements YggdrasilSerializable {
    private T gameruleValue;

    public GameruleValue(T gameruleValue) {
        this.gameruleValue = gameruleValue;
    }

    public T getGameruleValue() {
        return this.gameruleValue;
    }

    public String toString() {
        return this.gameruleValue.toString();
    }

    public boolean equals(@Nullable Object other) {
        if (!(other instanceof GameruleValue)) {
            return false;
        }
        return this.gameruleValue.equals(((GameruleValue)other).gameruleValue);
    }
}

