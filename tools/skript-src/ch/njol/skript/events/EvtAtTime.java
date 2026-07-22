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
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Time;
import ch.njol.util.Math2;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class EvtAtTime
extends SkriptEvent
implements Comparable<EvtAtTime> {
    private static final int CHECK_PERIOD = 10;
    private static final Map<World, EvtAtInfo> TRIGGERS;
    private int time;
    private World[] worlds;
    private static int taskID;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.time = ((Time)args[0].getSingle()).getTicks();
        this.worlds = args[1] == null ? Bukkit.getWorlds().toArray(new World[0]) : (World[])args[1].getAll();
        return true;
    }

    @Override
    public boolean postLoad() {
        for (World world : this.worlds) {
            EvtAtInfo info = TRIGGERS.get(world);
            if (info == null) {
                info = new EvtAtInfo();
                TRIGGERS.put(world, info);
                info.lastCheckedTime = (int)world.getTime() - 1;
            }
            info.instances.add(this);
        }
        EvtAtTime.registerListener();
        return true;
    }

    @Override
    public void unload() {
        Iterator<EvtAtInfo> iterator = TRIGGERS.values().iterator();
        while (iterator.hasNext()) {
            EvtAtInfo info = iterator.next();
            info.instances.remove(this);
            if (!info.instances.isEmpty()) continue;
            iterator.remove();
        }
        if (taskID != -1 && TRIGGERS.isEmpty()) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
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

    private static void registerListener() {
        if (taskID != -1) {
            return;
        }
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)Skript.getInstance(), () -> {
            for (Map.Entry<World, EvtAtInfo> entry : TRIGGERS.entrySet()) {
                boolean midnight;
                EvtAtInfo info = entry.getValue();
                int worldTime = (int)entry.getKey().getTime();
                if (info.lastCheckedTime == worldTime) continue;
                if (info.lastCheckedTime + 20 < worldTime || info.lastCheckedTime > worldTime && info.lastCheckedTime - 24000 + 20 < worldTime) {
                    info.lastCheckedTime = Math2.mod(worldTime - 10, 24000);
                }
                boolean bl = midnight = info.lastCheckedTime > worldTime;
                if (midnight) {
                    info.lastCheckedTime -= 24000;
                }
                for (EvtAtTime event : info.instances) {
                    int eventTime;
                    int n = eventTime = midnight && event.time > 12000 ? event.time - 24000 : event.time;
                    if (eventTime > worldTime) break;
                    if (eventTime <= info.lastCheckedTime) continue;
                    ScheduledEvent scheduledEvent = new ScheduledEvent(entry.getKey());
                    SkriptEventHandler.logEventStart(scheduledEvent);
                    SkriptEventHandler.logTriggerEnd(event.trigger);
                    event.trigger.execute(scheduledEvent);
                    SkriptEventHandler.logTriggerEnd(event.trigger);
                    SkriptEventHandler.logEventEnd();
                }
                info.lastCheckedTime = worldTime;
            }
        }, 0L, 10L);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "at " + Time.toString(this.time) + " in worlds " + Classes.toString(this.worlds, true);
    }

    @Override
    public int compareTo(@Nullable EvtAtTime event) {
        return event == null ? this.time : this.time - event.time;
    }

    static {
        Skript.registerEvent("*At Time", EvtAtTime.class, ScheduledEvent.class, "at %time% [in %worlds%]").description("An event that occurs at a given <a href='#time'>minecraft time</a> in every world or only in specific worlds.").examples("at 18:00", "at 7am in \"world\"").since("1.3.4");
        TRIGGERS = new ConcurrentHashMap<World, EvtAtInfo>();
        taskID = -1;
    }

    private static final class EvtAtInfo {
        private int lastCheckedTime;
        private final PriorityQueue<EvtAtTime> instances = new PriorityQueue(EvtAtTime::compareTo);

        private EvtAtInfo() {
        }
    }
}

