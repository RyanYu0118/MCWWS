/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.variables.Variables;
import ch.njol.skript.variables.VariablesMap;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class IndeterminateDelay
extends Delay {
    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        this.debug(event, true);
        long start = Skript.debug() ? System.nanoTime() : 0L;
        TriggerItem next = this.getNext();
        if (next != null && Skript.getInstance().isEnabled()) {
            Timespan duration = (Timespan)this.duration.getSingle(event);
            if (duration == null) {
                return null;
            }
            VariablesMap localVars = Variables.removeLocals(event);
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), () -> {
                Delay.addDelayedEvent(event);
                Skript.debug(this.getIndentation() + "... continuing after " + (double)(System.nanoTime() - start) / 1.0E9 + "s");
                if (localVars != null) {
                    Variables.setLocalVariables(event, localVars);
                }
                TriggerItem.walk(next, event);
            }, duration.getAs(Timespan.TimePeriod.TICK));
        }
        return null;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "wait for operation to finish";
    }
}

