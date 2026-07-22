/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.SkriptConfig;
import ch.njol.skript.util.Timespan;
import ch.njol.yggdrasil.YggdrasilSerializable;
import java.util.TimeZone;
import org.jetbrains.annotations.Nullable;

public class Date
extends java.util.Date
implements YggdrasilSerializable {
    public static Date now() {
        return new Date();
    }

    public static Date fromJavaDate(java.util.Date date) {
        if (date instanceof Date) {
            Date ours = (Date)date;
            return ours;
        }
        return new Date(date.getTime());
    }

    public Date() {
    }

    public Date(long timestamp) {
        super(timestamp);
    }

    public Date(long timestamp, TimeZone zone) {
        super(timestamp - (long)zone.getOffset(timestamp));
    }

    public void add(Timespan other) {
        this.setTime(this.getTime() + other.getAs(Timespan.TimePeriod.MILLISECOND));
    }

    public void subtract(Timespan other) {
        this.setTime(this.getTime() - other.getAs(Timespan.TimePeriod.MILLISECOND));
    }

    public Timespan difference(Date other) {
        return new Timespan(Math.abs(this.getTime() - other.getTime()));
    }

    public Date plus(Timespan other) {
        return new Date(this.getTime() + other.getAs(Timespan.TimePeriod.MILLISECOND));
    }

    public Date minus(Timespan other) {
        return new Date(this.getTime() - other.getAs(Timespan.TimePeriod.MILLISECOND));
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public long getTimestamp() {
        return this.getTime();
    }

    @Override
    public int hashCode() {
        return 31 + Long.hashCode(this.getTime());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof java.util.Date)) {
            return false;
        }
        java.util.Date other = (java.util.Date)obj;
        if (this == obj) {
            return true;
        }
        return this.getTime() == other.getTime();
    }

    @Override
    public String toString() {
        return SkriptConfig.formatDate(this.getTime());
    }
}

