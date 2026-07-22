/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.log;

import ch.njol.skript.Skript;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.LoggerFilter;
import java.util.logging.Filter;

public class BukkitLoggerFilter {
    private static final LoggerFilter filter = new LoggerFilter(SkriptLogger.LOGGER);

    public static void addFilter(Filter f) {
        filter.addFilter(f);
    }

    public static boolean removeFilter(Filter f) {
        return filter.removeFilter(f);
    }

    static {
        Skript.closeOnDisable(filter);
    }
}

