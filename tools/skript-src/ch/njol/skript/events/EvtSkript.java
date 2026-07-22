/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.events.bukkit.SkriptStartEvent;
import ch.njol.skript.events.bukkit.SkriptStopEvent;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EvtSkript
extends SkriptEvent {
    private static final List<Trigger> START;
    private static final List<Trigger> STOP;
    private boolean isStart;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void onSkriptStart() {
        SkriptStartEvent event = new SkriptStartEvent();
        List<Trigger> list = START;
        synchronized (list) {
            for (Trigger trigger : START) {
                trigger.execute(event);
            }
            START.clear();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void onSkriptStop() {
        SkriptStopEvent event = new SkriptStopEvent();
        List<Trigger> list = STOP;
        synchronized (list) {
            for (Trigger trigger : STOP) {
                trigger.execute(event);
            }
            STOP.clear();
        }
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        boolean bl = this.isStart = matchedPattern == 0;
        if (parseResult.hasTag("server")) {
            Skript.warning("Server start/stop events are actually called when Skript is started or stopped.It is thus recommended to use 'on Skript start/stop' instead.");
        }
        return true;
    }

    @Override
    public boolean postLoad() {
        (this.isStart ? START : STOP).add(this.trigger);
        return true;
    }

    @Override
    public void unload() {
        (this.isStart ? START : STOP).remove(this.trigger);
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
        return "on skript " + (this.isStart ? "start" : "stop");
    }

    static {
        Skript.registerEvent("Server Start/Stop", EvtSkript.class, CollectionUtils.array(SkriptStartEvent.class, SkriptStopEvent.class), "(:server|skript) (start|load|enable)", "(:server|skript) (stop|unload|disable)").description("Called when the server starts or stops (actually, when Skript starts or stops, so a /reload will trigger these events as well).").examples("on skript start:", "on server stop:").since("2.0");
        START = Collections.synchronizedList(new ArrayList());
        STOP = Collections.synchronizedList(new ArrayList());
    }
}

