/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptEventHandler;
import ch.njol.skript.events.bukkit.ScheduledEvent;
import ch.njol.skript.events.bukkit.ScheduledNoWorldEvent;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Timespan;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class EvtPeriodical
extends SkriptEvent {
    private Timespan period;
    private int[] taskIDs;
    private World @Nullable [] worlds;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.period = (Timespan)args[0].getSingle();
        if (args.length > 1 && args[1] != null) {
            this.worlds = (World[])args[1].getArray();
        }
        return true;
    }

    @Override
    public boolean postLoad() {
        long ticks = this.period.getAs(Timespan.TimePeriod.TICK);
        if (this.worlds == null) {
            this.taskIDs = new int[]{Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)Skript.getInstance(), () -> this.execute(null), ticks, ticks)};
        } else {
            this.taskIDs = new int[this.worlds.length];
            for (int i = 0; i < this.worlds.length; ++i) {
                World world = this.worlds[i];
                this.taskIDs[i] = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)Skript.getInstance(), () -> this.execute(world), ticks - world.getFullTime() % ticks, ticks);
            }
        }
        return true;
    }

    @Override
    public void unload() {
        for (int taskID : this.taskIDs) {
            Bukkit.getScheduler().cancelTask(taskID);
        }
    }

    @Override
    public boolean check(Event event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEventPrioritySupported() {
        return false;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "every " + String.valueOf(this.period);
    }

    private void execute(@Nullable World world) {
        ScheduledEvent event = world == null ? new ScheduledNoWorldEvent() : new ScheduledEvent(world);
        SkriptEventHandler.logEventStart(event);
        SkriptEventHandler.logTriggerStart(this.trigger);
        this.trigger.execute(event);
        SkriptEventHandler.logTriggerEnd(this.trigger);
        SkriptEventHandler.logEventEnd();
    }

    static {
        Skript.registerEvent("*Periodical", EvtPeriodical.class, ScheduledNoWorldEvent.class, "every %timespan%").description("An event that is called periodically.").examples("every 2 seconds:", "every minecraft hour:", "every tick: # can cause lag depending on the code inside the event", "every minecraft days:").since("1.0");
        Skript.registerEvent("*Periodical", EvtPeriodical.class, ScheduledEvent.class, "every %timespan% in [world[s]] %worlds%").description("An event that is called periodically.").examples("every 2 seconds in \"world\":", "every minecraft hour in \"flatworld\":", "every tick in \"world\": # can cause lag depending on the code inside the event", "every minecraft days in \"plots\":").since("1.0").documentationID("eventperiodical");
    }
}

