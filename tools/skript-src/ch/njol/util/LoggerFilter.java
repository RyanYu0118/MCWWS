/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util;

import ch.njol.util.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public final class LoggerFilter
implements Filter,
Closeable {
    private final Logger l;
    private final Collection<Filter> filters = new ArrayList<Filter>(5);
    @Nullable
    private final Filter oldFilter;

    public LoggerFilter(Logger l) {
        this.l = l;
        this.oldFilter = l.getFilter();
        l.setFilter(this);
    }

    @Override
    public boolean isLoggable(@Nullable LogRecord record) {
        if (this.oldFilter != null && !this.oldFilter.isLoggable(record)) {
            return false;
        }
        for (Filter f : this.filters) {
            if (f.isLoggable(record)) continue;
            return false;
        }
        return true;
    }

    public final void addFilter(Filter f) {
        this.filters.add(f);
    }

    public final boolean removeFilter(Filter f) {
        return this.filters.remove(f);
    }

    @Override
    public void close() {
        this.l.setFilter(this.oldFilter);
    }
}

