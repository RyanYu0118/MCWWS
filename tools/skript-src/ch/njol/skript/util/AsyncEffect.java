/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.Skript;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.timings.SkriptTimings;
import ch.njol.skript.variables.Variables;
import ch.njol.skript.variables.VariablesMap;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class AsyncEffect
extends Effect {
    @Override
    @Nullable
    protected TriggerItem walk(Event e) {
        this.debug(e, true);
        VariablesMap localVars = Variables.removeLocals(e);
        if (!Skript.getInstance().isEnabled()) {
            return null;
        }
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)Skript.getInstance(), () -> {
            Delay.addDelayedEvent(e);
            if (localVars != null) {
                Variables.setLocalVariables(e, localVars);
            }
            this.execute(e);
            if (this.getNext() != null) {
                Bukkit.getScheduler().runTask((Plugin)Skript.getInstance(), () -> {
                    Trigger trigger;
                    Object timing = null;
                    if (SkriptTimings.enabled() && (trigger = this.getTrigger()) != null) {
                        timing = SkriptTimings.start(trigger.getDebugLabel());
                    }
                    TriggerItem.walk(this.getNext(), e);
                    Variables.removeLocals(e);
                    SkriptTimings.stop(timing);
                });
            } else {
                Variables.removeLocals(e);
            }
        });
        return null;
    }
}

