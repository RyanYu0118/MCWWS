/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.UnmodifiableView
 */
package org.skriptlang.skript.log.runtime;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.skriptlang.skript.log.runtime.ErrorSource;
import org.skriptlang.skript.log.runtime.RuntimeError;

public final class Frame {
    private final FrameLimit limits;
    private int printed;
    private final Map<ErrorSource.Location, Integer> lineTotals;
    private final Map<ErrorSource.Location, Integer> lineSkipped;
    private final Map<ErrorSource.Location, Integer> timeouts;

    public Frame(FrameLimit limits) {
        this.limits = limits;
        this.lineTotals = new ConcurrentHashMap<ErrorSource.Location, Integer>();
        this.lineSkipped = new ConcurrentHashMap<ErrorSource.Location, Integer>();
        this.timeouts = new ConcurrentHashMap<ErrorSource.Location, Integer>();
    }

    public boolean add(@NotNull RuntimeError error) {
        ErrorSource.Location location = error.source().location();
        int lineTotal = this.lineTotals.compute(location, (key, count) -> count == null ? 1 : count + 1);
        if (this.timeouts.containsKey(location)) {
            this.lineSkipped.compute(location, (key, count) -> count == null ? 1 : count + 1);
            return false;
        }
        if (this.printed < this.limits.totalLimit && lineTotal <= this.limits.lineLimit) {
            ++this.printed;
            return true;
        }
        this.lineSkipped.compute(location, (key, count) -> count == null ? 1 : count + 1);
        if (lineTotal == this.limits.lineTimeoutLimit) {
            this.timeouts.put(location, this.limits.timeoutDuration);
        }
        return false;
    }

    public void nextFrame() {
        this.printed = 0;
        this.lineTotals.clear();
        this.lineSkipped.clear();
        Iterator<Map.Entry<ErrorSource.Location, Integer>> it = this.timeouts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ErrorSource.Location, Integer> entry = it.next();
            if (entry.getValue() > 0) {
                entry.setValue(entry.getValue() - 1);
                continue;
            }
            it.remove();
        }
    }

    @Contract(value=" -> new")
    @NotNull
    public FrameOutput getFrameOutput() {
        HashSet<ErrorSource.Location> newTimeouts = new HashSet<ErrorSource.Location>();
        for (Map.Entry<ErrorSource.Location, Integer> entry : this.timeouts.entrySet()) {
            if (entry.getValue() != this.limits.timeoutDuration) continue;
            newTimeouts.add(entry.getKey());
        }
        return new FrameOutput(Collections.unmodifiableMap(this.lineTotals), Collections.unmodifiableMap(this.lineSkipped), Collections.unmodifiableSet(newTimeouts), this.limits);
    }

    public record FrameLimit(int totalLimit, int lineLimit, int lineTimeoutLimit, int timeoutDuration) {
    }

    public record FrameOutput(@UnmodifiableView Map<ErrorSource.Location, Integer> totalErrors, @UnmodifiableView Map<ErrorSource.Location, Integer> skippedErrors, @UnmodifiableView Set<ErrorSource.Location> newTimeouts, FrameLimit frameLimits) {
    }
}

