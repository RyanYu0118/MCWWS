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
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.timings.SkriptTimings;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.variables.Variables;
import ch.njol.skript.variables.VariablesMap;
import ch.njol.util.Kleenean;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

@Name(value="Delay")
@Description(value={"Delays the script's execution by a given timespan. Please note that delays are not persistent, e.g. trying to create a tempban script with <code>ban player \u2192 wait 7 days \u2192 unban player</code> will not work if you restart your server anytime within these 7 days. You also have to be careful even when using small delays!"})
@Example.Examples(value={@Example(value="wait 2 minutes"), @Example(value="halt for 5 minecraft hours"), @Example(value="wait a tick")})
@Since(value={"1.4"})
public class Delay
extends Effect {
    protected Expression<Timespan> duration;
    private static final Set<Event> DELAYED;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.getParser().setHasDelayBefore(Kleenean.TRUE);
        this.duration = exprs[0];
        if (this.duration instanceof Literal) {
            Timespan timespan = (Timespan)((Literal)this.duration).getSingle();
            if (timespan.isInfinite()) {
                Skript.error("Delaying for an eternity is not allowed. Use the 'stop' effect instead.");
                return false;
            }
            long millis = timespan.getAs(Timespan.TimePeriod.MILLISECOND);
            if (millis < 50L) {
                Skript.warning("Delays less than one tick are not possible, defaulting to one tick.");
            }
        }
        return true;
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        this.debug(event, true);
        long start = Skript.debug() ? System.nanoTime() : 0L;
        TriggerItem next = this.getNext();
        if (next != null && Skript.getInstance().isEnabled()) {
            Timespan duration = this.duration.getSingle(event);
            if (duration == null) {
                return null;
            }
            VariablesMap localVars = Variables.removeLocals(event);
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)Skript.getInstance(), () -> {
                Trigger trigger;
                Delay.addDelayedEvent(event);
                Skript.debug(this.getIndentation() + "... continuing after " + (double)(System.nanoTime() - start) / 1.0E9 + "s");
                if (localVars != null) {
                    Variables.setLocalVariables(event, localVars);
                }
                Object timing = null;
                if (SkriptTimings.enabled() && (trigger = this.getTrigger()) != null) {
                    timing = SkriptTimings.start(trigger.getDebugLabel());
                }
                TriggerItem.walk(next, event);
                Variables.removeLocals(event);
                SkriptTimings.stop(timing);
            }, Math.max(duration.getAs(Timespan.TimePeriod.TICK), 1L));
        }
        return null;
    }

    @Override
    protected void execute(Event event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "wait for " + this.duration.toString(event, debug) + (event == null ? "" : "...");
    }

    public static boolean isDelayed(Event event) {
        return DELAYED.contains(event);
    }

    public static void addDelayedEvent(Event event) {
        DELAYED.add(event);
    }

    static {
        Skript.registerEffect(Delay.class, "(wait|halt) [for] %timespan%");
        DELAYED = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap()));
    }
}

