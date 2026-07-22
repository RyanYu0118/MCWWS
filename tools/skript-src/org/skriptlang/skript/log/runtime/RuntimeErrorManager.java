/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.log.runtime;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.util.Task;
import ch.njol.skript.util.Timespan;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.log.runtime.Frame;
import org.skriptlang.skript.log.runtime.RuntimeError;
import org.skriptlang.skript.log.runtime.RuntimeErrorConsumer;
import org.skriptlang.skript.log.runtime.RuntimeErrorFilter;

public class RuntimeErrorManager
implements Closeable {
    private static RuntimeErrorManager instance;
    static RuntimeErrorFilter standardFilter;
    private final Task task;
    private final Map<RuntimeErrorFilter, Set<RuntimeErrorConsumer>> filterMap = new ConcurrentHashMap<RuntimeErrorFilter, Set<RuntimeErrorConsumer>>();

    @ApiStatus.Internal
    public static RuntimeErrorManager getInstance() {
        return instance;
    }

    public static void refresh() {
        long frameLength = SkriptConfig.runtimeErrorFrameDuration.value().getAs(Timespan.TimePeriod.TICK);
        if (instance == null) {
            instance = new RuntimeErrorManager(frameLength);
        } else {
            Map<RuntimeErrorFilter, Set<RuntimeErrorConsumer>> oldMap = RuntimeErrorManager.instance.filterMap;
            instance = new RuntimeErrorManager(frameLength);
            RuntimeErrorManager.instance.filterMap.putAll(oldMap);
        }
        int errorLimit = SkriptConfig.runtimeErrorLimitTotal.value();
        int errorLineLimit = SkriptConfig.runtimeErrorLimitLine.value();
        int errorLineTimeout = SkriptConfig.runtimeErrorLimitLineTimeout.value();
        int errorTimeoutLength = Math.max(SkriptConfig.runtimeErrorTimeoutDuration.value(), 1);
        Frame.FrameLimit errorLimits = new Frame.FrameLimit(errorLimit, errorLineLimit, errorLineTimeout, errorTimeoutLength);
        int warningLimit = SkriptConfig.runtimeWarningLimitTotal.value();
        int warningLineLimit = SkriptConfig.runtimeWarningLimitLine.value();
        int warningLineTimeout = SkriptConfig.runtimeWarningLimitLineTimeout.value();
        int warningTimeoutLength = Math.max(SkriptConfig.runtimeWarningTimeoutDuration.value(), 1);
        Frame.FrameLimit warningsLimits = new Frame.FrameLimit(warningLimit, warningLineLimit, warningLineTimeout, warningTimeoutLength);
        if (standardFilter == null) {
            standardFilter = new RuntimeErrorFilter(errorLimits, warningsLimits);
        } else {
            standardFilter.setErrorFrameLimits(errorLimits);
            standardFilter.setWarningFrameLimits(warningsLimits);
        }
    }

    public RuntimeErrorManager(long frameLength) {
        this.task = new Task((Plugin)Skript.getInstance(), frameLength, frameLength, true){

            @Override
            public void run() {
                for (Map.Entry<RuntimeErrorFilter, Set<RuntimeErrorConsumer>> entry : RuntimeErrorManager.this.filterMap.entrySet()) {
                    RuntimeErrorFilter filter = entry.getKey();
                    if (filter == null) continue;
                    Set<RuntimeErrorConsumer> consumers = entry.getValue();
                    Frame errorFrame = filter.getErrorFrame();
                    consumers.forEach(consumer -> consumer.printFrameOutput(errorFrame.getFrameOutput(), Level.SEVERE));
                    errorFrame.nextFrame();
                    Frame warningFrame = filter.getErrorFrame();
                    consumers.forEach(consumer -> consumer.printFrameOutput(warningFrame.getFrameOutput(), Level.WARNING));
                    warningFrame.nextFrame();
                }
            }
        };
    }

    public void error(@NotNull RuntimeError error) {
        for (Map.Entry<RuntimeErrorFilter, Set<RuntimeErrorConsumer>> entry : this.filterMap.entrySet()) {
            RuntimeErrorFilter filter = entry.getKey();
            Set<RuntimeErrorConsumer> consumers = entry.getValue();
            if (filter != null && !filter.test(error)) continue;
            consumers.forEach(consumer -> consumer.printError(error));
        }
    }

    @Deprecated(since="2.13", forRemoval=true)
    public Frame getErrorFrame() {
        return standardFilter.getErrorFrame();
    }

    @Deprecated(since="2.13", forRemoval=true)
    public Frame getWarningFrame() {
        return standardFilter.getWarningFrame();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addConsumer(RuntimeErrorConsumer consumer) {
        Map<RuntimeErrorFilter, Set<RuntimeErrorConsumer>> map = this.filterMap;
        synchronized (map) {
            this.filterMap.computeIfAbsent(consumer.getFilter(), key -> new HashSet()).add(consumer);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addConsumers(RuntimeErrorConsumer ... newConsumers) {
        Map<RuntimeErrorFilter, Set<RuntimeErrorConsumer>> map = this.filterMap;
        synchronized (map) {
            for (RuntimeErrorConsumer consumer : newConsumers) {
                this.filterMap.computeIfAbsent(consumer.getFilter(), key -> new HashSet()).add(consumer);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean removeConsumer(RuntimeErrorConsumer consumer) {
        Map<RuntimeErrorFilter, Set<RuntimeErrorConsumer>> map = this.filterMap;
        synchronized (map) {
            Set<RuntimeErrorConsumer> set = this.filterMap.get(consumer.getFilter());
            if (set == null) {
                return false;
            }
            boolean removed = set.remove(consumer);
            if (set.isEmpty()) {
                this.filterMap.remove(consumer.getFilter());
            }
            return removed;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<RuntimeErrorConsumer> removeAllConsumers() {
        Map<RuntimeErrorFilter, Set<RuntimeErrorConsumer>> map = this.filterMap;
        synchronized (map) {
            ArrayList<RuntimeErrorConsumer> currentConsumers = new ArrayList<RuntimeErrorConsumer>();
            for (Set<RuntimeErrorConsumer> set : this.filterMap.values()) {
                currentConsumers.addAll(set);
            }
            this.filterMap.clear();
            return currentConsumers;
        }
    }

    @Override
    public void close() {
        this.task.close();
    }
}

