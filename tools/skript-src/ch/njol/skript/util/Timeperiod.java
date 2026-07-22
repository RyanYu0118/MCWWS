/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.util.Time;
import ch.njol.yggdrasil.YggdrasilSerializable;
import org.jetbrains.annotations.Nullable;

public class Timeperiod
implements YggdrasilSerializable {
    public final int start;
    public final int end;

    public Timeperiod() {
        this.end = 0;
        this.start = 0;
    }

    public Timeperiod(int start, int end) {
        this.start = (start + 24000) % 24000;
        this.end = (end + 24000) % 24000;
    }

    public Timeperiod(int time) {
        this.start = this.end = (time + 24000) % 24000;
    }

    public boolean contains(int time) {
        return this.start <= this.end ? time >= this.start && time <= this.end : time <= this.end || time >= this.start;
    }

    public boolean contains(Time t) {
        return this.contains(t.getTicks());
    }

    public String toString() {
        return Time.toString(this.start) + (String)(this.start == this.end ? "" : "-" + Time.toString(this.end));
    }

    public int hashCode() {
        return this.start + this.end << 16;
    }

    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Timeperiod)) {
            return false;
        }
        Timeperiod other = (Timeperiod)obj;
        return this.end == other.end && this.start == other.start;
    }
}

