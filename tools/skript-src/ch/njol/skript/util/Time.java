/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.Message;
import ch.njol.skript.util.Utils;
import ch.njol.util.Math2;
import ch.njol.yggdrasil.YggdrasilSerializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.util.Cyclical;

public class Time
implements YggdrasilSerializable,
Cyclical<Integer> {
    private static final int TICKS_PER_HOUR = 1000;
    private static final int TICKS_PER_DAY = 24000;
    private static final double TICKS_PER_MINUTE = 16.666666666666668;
    private static final int HOUR_ZERO = 6000;
    private final int time;
    private static final Pattern DAY_TIME_PATTERN = Pattern.compile("(\\d?\\d)(:(\\d\\d))? ?(am|pm)", 2);
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d?\\d:\\d\\d", 2);
    private static final Message m_error_24_hours = new Message("time.errors.24 hours");
    private static final Message m_error_12_hours = new Message("time.errors.12 hours");
    private static final Message m_error_60_minutes = new Message("time.errors.60 minutes");

    public Time() {
        this(0);
    }

    public Time(int time) {
        this.time = Math2.mod(time, 24000);
    }

    public int getTicks() {
        return this.time;
    }

    public int getTime() {
        return (this.time + 6000) % 24000;
    }

    public int getHour() {
        int hour;
        int thisTime = this.time;
        if (this.time < 0) {
            thisTime = Math.abs(this.time);
        }
        if ((hour = (thisTime + 6000) / 1000) >= 24) {
            hour -= 24;
        }
        return hour;
    }

    public int getMinute() {
        return (int)Math2.round((double)((this.time + 6000) % 1000) / 16.666666666666668);
    }

    public String toString() {
        return Time.toString(this.time);
    }

    public static String toString(int ticks) {
        assert (0 <= ticks && ticks < 24000);
        int t = (ticks + 6000) % 24000;
        int hours = t / 1000;
        int minutes = (int)Math.round((double)(t % 1000) / 16.666666666666668);
        if (minutes >= 60) {
            hours = (hours + 1) % 24;
            minutes -= 60;
        }
        return hours + ":" + (minutes < 10 ? "0" : "") + minutes;
    }

    @Nullable
    public static Time parse(String s) {
        if (TIME_PATTERN.matcher(s).matches()) {
            int hours = Utils.parseInt(s.split(":")[0]);
            if (hours == 24) {
                hours = 0;
            } else if (hours > 24) {
                Skript.error(String.valueOf(m_error_24_hours));
                return null;
            }
            int minutes = Utils.parseInt(s.split(":")[1]);
            if (minutes >= 60) {
                Skript.error(String.valueOf(m_error_60_minutes));
                return null;
            }
            return new Time((int)Math.round((double)(hours * 1000 - 6000) + (double)minutes * 16.666666666666668));
        }
        Matcher m = DAY_TIME_PATTERN.matcher(s);
        if (m.matches()) {
            int hours = Utils.parseInt(m.group(1));
            if (hours == 12) {
                hours = 0;
            } else if (hours > 12) {
                Skript.error(String.valueOf(m_error_12_hours));
                return null;
            }
            int minutes = 0;
            if (m.group(3) != null) {
                minutes = Utils.parseInt(m.group(3));
            }
            if (minutes >= 60) {
                Skript.error(String.valueOf(m_error_60_minutes));
                return null;
            }
            if (m.group(4).equalsIgnoreCase("pm")) {
                hours += 12;
            }
            return new Time((int)Math.round((double)(hours * 1000 - 6000) + (double)minutes * 16.666666666666668));
        }
        return null;
    }

    public int hashCode() {
        return this.time;
    }

    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Time)) {
            return false;
        }
        Time other = (Time)obj;
        return this.time == other.time;
    }

    @Override
    public Integer getMaximum() {
        return 24000;
    }

    @Override
    public Integer getMinimum() {
        return 0;
    }
}

