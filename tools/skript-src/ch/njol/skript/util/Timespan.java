/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.localization.GeneralWords;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.util.Utils;
import ch.njol.util.Math2;
import ch.njol.util.NonNullPair;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.yggdrasil.YggdrasilSerializable;
import com.google.common.base.Preconditions;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Timespan
implements YggdrasilSerializable,
Comparable<Timespan>,
TemporalAmount {
    private static final Pattern TIMESPAN_PATTERN = Pattern.compile("^(\\d+):(\\d\\d)(:\\d\\d){0,2}(?<ms>\\.\\d{1,4})?$");
    private static final Pattern TIMESPAN_NUMBER_PATTERN = Pattern.compile("^\\d+(\\.\\d+)?$");
    private static final Pattern TIMESPAN_SPLIT_PATTERN = Pattern.compile("[:.]");
    private static final Pattern SHORT_FORM_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)([a-zA-Z]+)$");
    private static final Noun FOREVER_NAME = new Noun("time.forever");
    private static final List<NonNullPair<Noun, Long>> SIMPLE_VALUES = Arrays.asList(new NonNullPair<Noun, Long>(TimePeriod.YEAR.name, TimePeriod.YEAR.time), new NonNullPair<Noun, Long>(TimePeriod.MONTH.name, TimePeriod.MONTH.time), new NonNullPair<Noun, Long>(TimePeriod.WEEK.name, TimePeriod.WEEK.time), new NonNullPair<Noun, Long>(TimePeriod.DAY.name, TimePeriod.DAY.time), new NonNullPair<Noun, Long>(TimePeriod.HOUR.name, TimePeriod.HOUR.time), new NonNullPair<Noun, Long>(TimePeriod.MINUTE.name, TimePeriod.MINUTE.time), new NonNullPair<Noun, Long>(TimePeriod.SECOND.name, TimePeriod.SECOND.time));
    private static final Map<String, Long> PARSE_VALUES = new HashMap<String, Long>();
    private final long millis;

    @Nullable
    public static Timespan parse(String value) {
        return Timespan.parse(value, ParseContext.DEFAULT);
    }

    @Nullable
    public static Timespan parse(String value, ParseContext context) {
        if (value.isEmpty()) {
            return null;
        }
        long totalMillis = 0L;
        boolean minecraftTime = false;
        boolean isMinecraftTimeSet = false;
        Matcher matcher = TIMESPAN_PATTERN.matcher(value);
        if (matcher.matches()) {
            String[] substring = TIMESPAN_SPLIT_PATTERN.split(value);
            long[] times = new long[]{1L, TimePeriod.SECOND.time, TimePeriod.MINUTE.time, TimePeriod.HOUR.time, TimePeriod.DAY.time};
            boolean hasMs = value.contains(".");
            int length = substring.length;
            int offset = 2;
            if (length == 4 && !hasMs || length == 5) {
                offset = 0;
            } else if (length == 3 && !hasMs || length == 4) {
                offset = 1;
            }
            for (int i = 0; i < substring.length; ++i) {
                totalMillis += times[offset + i] * Utils.parseLong(substring[i]);
            }
        } else {
            String[] substring = value.toLowerCase(Locale.ENGLISH).split("\\s+");
            for (int i = 0; i < substring.length; ++i) {
                Long millis;
                Matcher shortFormMatcher;
                String sub = substring[i];
                if (sub.equals(GeneralWords.and.toString())) {
                    if (i != 0 && i != substring.length - 1) continue;
                    return null;
                }
                double amount = 1.0;
                if (Noun.isIndefiniteArticle(sub)) {
                    if (i == substring.length - 1) {
                        return null;
                    }
                    sub = substring[++i];
                } else if (TIMESPAN_NUMBER_PATTERN.matcher(sub).matches()) {
                    if (i == substring.length - 1) {
                        return null;
                    }
                    try {
                        amount = Double.parseDouble(sub);
                    }
                    catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid timespan: " + value);
                    }
                    sub = substring[++i];
                }
                if (CollectionUtils.contains(Language.getList("time.real"), sub)) {
                    if (i == substring.length - 1 || isMinecraftTimeSet && minecraftTime) {
                        return null;
                    }
                    sub = substring[++i];
                } else if (CollectionUtils.contains(Language.getList("time.minecraft"), sub)) {
                    if (i == substring.length - 1 || isMinecraftTimeSet && !minecraftTime) {
                        return null;
                    }
                    minecraftTime = true;
                    sub = substring[++i];
                }
                if (sub.endsWith(",")) {
                    sub = sub.substring(0, sub.length() - 1);
                }
                if (context == ParseContext.COMMAND && (shortFormMatcher = SHORT_FORM_PATTERN.matcher(sub)).matches()) {
                    amount = Double.parseDouble(shortFormMatcher.group(1));
                    sub = shortFormMatcher.group(2).toLowerCase(Locale.ENGLISH);
                }
                if ((millis = PARSE_VALUES.get(sub.toLowerCase(Locale.ENGLISH))) == null) {
                    return null;
                }
                if (minecraftTime && millis != TimePeriod.TICK.time) {
                    amount /= 72.0;
                }
                totalMillis += Math.round(amount * (double)millis.longValue());
                isMinecraftTimeSet = true;
            }
        }
        return new Timespan(totalMillis);
    }

    @Contract(value="_ -> new")
    @NotNull
    public static Timespan fromDuration(@NotNull Duration duration) {
        return new Timespan(duration.toMillis());
    }

    @Contract(value=" -> new", pure=true)
    @NotNull
    public static Timespan infinite() {
        return new Timespan(Long.MAX_VALUE);
    }

    public static String toString(long millis) {
        return Timespan.toString(millis, 0);
    }

    public static String toString(long millis, int flags) {
        if (millis == Long.MAX_VALUE) {
            return FOREVER_NAME.toString(false);
        }
        for (int i = 0; i < SIMPLE_VALUES.size() - 1; ++i) {
            NonNullPair<Noun, Long> pair = SIMPLE_VALUES.get(i);
            long second1 = pair.getSecond();
            if (millis < second1) continue;
            long remainder = millis % second1;
            double second = 1.0 * (double)remainder / (double)SIMPLE_VALUES.get(i + 1).getSecond().longValue();
            if (!"0".equals(Skript.toString(second))) {
                return Timespan.toString(Math.floor(1.0 * (double)millis / (double)second1), pair, flags) + " " + String.valueOf(GeneralWords.and) + " " + Timespan.toString(remainder, flags);
            }
            return Timespan.toString(1.0 * (double)millis / (double)second1, pair, flags);
        }
        return Timespan.toString(1.0 * (double)millis / (double)SIMPLE_VALUES.get(SIMPLE_VALUES.size() - 1).getSecond().longValue(), SIMPLE_VALUES.get(SIMPLE_VALUES.size() - 1), flags);
    }

    private static String toString(double amount, NonNullPair<Noun, Long> pair, int flags) {
        return pair.getFirst().withAmount(amount, flags);
    }

    public Timespan() {
        this.millis = 0L;
    }

    public Timespan(long millis) {
        Preconditions.checkArgument((millis >= 0L ? 1 : 0) != 0, (Object)"millis must be >= 0");
        this.millis = millis;
    }

    public Timespan(TimePeriod timePeriod, long time) {
        Preconditions.checkArgument((time >= 0L ? 1 : 0) != 0, (Object)"time must be >= 0");
        this.millis = time * timePeriod.getTime();
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static Timespan fromTicks(long ticks) {
        return new Timespan(ticks * 50L);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public static Timespan fromTicks_i(long ticks) {
        return new Timespan(ticks * 50L);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public long getMilliSeconds() {
        return this.getAs(TimePeriod.MILLISECOND);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public long getTicks() {
        return this.getAs(TimePeriod.TICK);
    }

    @Deprecated(since="2.10.0", forRemoval=true)
    public long getTicks_i() {
        return this.getAs(TimePeriod.TICK);
    }

    public boolean isInfinite() {
        return this.millis == Long.MAX_VALUE;
    }

    public long getAs(TimePeriod timePeriod) {
        return this.millis / timePeriod.getTime();
    }

    public Duration getDuration() {
        return Duration.ofMillis(this.millis);
    }

    @Contract(value="_ -> new", pure=true)
    public Timespan add(Timespan timespan) {
        if (this.isInfinite() || timespan.isInfinite()) {
            return Timespan.infinite();
        }
        long millis = Math2.addClamped(this.millis, timespan.getAs(TimePeriod.MILLISECOND));
        return new Timespan(millis);
    }

    @Contract(value="_ -> new", pure=true)
    public Timespan subtract(Timespan timespan) {
        if (this.isInfinite() || timespan.isInfinite()) {
            return Timespan.infinite();
        }
        long millis = Math.max(0L, this.millis - timespan.getAs(TimePeriod.MILLISECOND));
        return new Timespan(millis);
    }

    @Contract(value="_ -> new", pure=true)
    public Timespan multiply(double scalar) {
        Preconditions.checkArgument((scalar >= 0.0 ? 1 : 0) != 0);
        if (Double.isInfinite(scalar)) {
            return Timespan.infinite();
        }
        double value = (double)this.getAs(TimePeriod.MILLISECOND) * scalar;
        return new Timespan((long)Math.min(value, 9.223372036854776E18));
    }

    @Contract(value="_ -> new", pure=true)
    public Timespan divide(double scalar) {
        Preconditions.checkArgument((scalar >= 0.0 ? 1 : 0) != 0, (Object)"Cannot divide a timespan by non-positive value");
        if (this.isInfinite()) {
            return Timespan.infinite();
        }
        double value = (double)this.getAs(TimePeriod.MILLISECOND) / scalar;
        if (Double.isNaN(value)) {
            return new Timespan(0L);
        }
        if (Double.isInfinite(value)) {
            return Timespan.infinite();
        }
        return new Timespan((long)Math.min(value, 9.223372036854776E18));
    }

    @Contract(pure=true)
    public double divide(Timespan other) {
        if (this.isInfinite()) {
            if (other.isInfinite()) {
                return Double.NaN;
            }
            return Double.POSITIVE_INFINITY;
        }
        if (other.isInfinite()) {
            return 0.0;
        }
        return (double)this.getAs(TimePeriod.MILLISECOND) / (double)other.getAs(TimePeriod.MILLISECOND);
    }

    public Timespan difference(Timespan timespan) {
        if (this.isInfinite() || timespan.isInfinite()) {
            return Timespan.infinite();
        }
        long millis = Math.abs(this.millis - timespan.getAs(TimePeriod.MILLISECOND));
        return new Timespan(millis);
    }

    @Override
    public long get(TemporalUnit unit) {
        if (unit instanceof TimePeriod) {
            TimePeriod period = (TimePeriod)unit;
            return this.getAs(period);
        }
        if (!(unit instanceof ChronoUnit)) {
            throw new UnsupportedTemporalTypeException("Not a supported temporal unit: " + String.valueOf(unit));
        }
        ChronoUnit chrono = (ChronoUnit)unit;
        return switch (chrono) {
            case ChronoUnit.MILLIS -> this.getAs(TimePeriod.MILLISECOND);
            case ChronoUnit.SECONDS -> this.getAs(TimePeriod.SECOND);
            case ChronoUnit.MINUTES -> this.getAs(TimePeriod.MINUTE);
            case ChronoUnit.HOURS -> this.getAs(TimePeriod.HOUR);
            case ChronoUnit.DAYS -> this.getAs(TimePeriod.DAY);
            case ChronoUnit.WEEKS -> this.getAs(TimePeriod.WEEK);
            case ChronoUnit.MONTHS -> this.getAs(TimePeriod.MONTH);
            case ChronoUnit.YEARS -> this.getAs(TimePeriod.YEAR);
            default -> throw new UnsupportedTemporalTypeException("Not a supported time unit: " + String.valueOf(chrono));
        };
    }

    @Override
    public List<TemporalUnit> getUnits() {
        return List.of(TimePeriod.values()).reversed();
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        return temporal.plus(this.millis, ChronoUnit.MILLIS);
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        return temporal.minus(this.millis, ChronoUnit.MILLIS);
    }

    @Override
    public int compareTo(@Nullable Timespan time) {
        return Long.compare(this.millis, time == null ? this.millis : time.millis);
    }

    public int hashCode() {
        return 31 + (int)(this.millis / Integer.MAX_VALUE);
    }

    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Timespan)) {
            return false;
        }
        Timespan other = (Timespan)obj;
        return this.millis == other.millis;
    }

    public String toString() {
        return Timespan.toString(this.millis);
    }

    public String toString(int flags) {
        return Timespan.toString(this.millis, flags);
    }

    static {
        Language.addListener(() -> {
            for (TimePeriod time : TimePeriod.values()) {
                PARSE_VALUES.put(time.name.getSingular().toLowerCase(Locale.ENGLISH), time.getTime());
                PARSE_VALUES.put(time.name.getPlural().toLowerCase(Locale.ENGLISH), time.getTime());
                PARSE_VALUES.put(time.shortName.getSingular().toLowerCase(Locale.ENGLISH), time.getTime());
                PARSE_VALUES.put(time.shortName.getPlural().toLowerCase(Locale.ENGLISH), time.getTime());
            }
        });
    }

    public static enum TimePeriod implements TemporalUnit
    {
        MILLISECOND(1L),
        TICK(50L),
        SECOND(1000L),
        MINUTE(TimePeriod.SECOND.time * 60L),
        HOUR(TimePeriod.MINUTE.time * 60L),
        DAY(TimePeriod.HOUR.time * 24L),
        WEEK(TimePeriod.DAY.time * 7L),
        MONTH(TimePeriod.DAY.time * 30L),
        YEAR(TimePeriod.DAY.time * 365L);

        private final Noun name = new Noun("time." + this.name().toLowerCase(Locale.ENGLISH) + ".full");
        private final Noun shortName = new Noun("time." + this.name().toLowerCase(Locale.ENGLISH) + ".short");
        private final long time;

        private TimePeriod(long time) {
            this.time = time;
        }

        public long getTime() {
            return this.time;
        }

        public String getFullForm() {
            return this.name.toString();
        }

        public String getShortForm() {
            return this.shortName.toString();
        }

        @Override
        public Duration getDuration() {
            return Duration.ofMillis(this.time);
        }

        @Override
        public boolean isDurationEstimated() {
            return false;
        }

        @Override
        public boolean isDateBased() {
            return false;
        }

        @Override
        public boolean isTimeBased() {
            return true;
        }

        @Override
        public <R extends Temporal> R addTo(R temporal, long amount) {
            return (R)temporal.plus(amount, this);
        }

        @Override
        public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
            return temporal1Inclusive.until(temporal2Exclusive, this);
        }
    }
}

