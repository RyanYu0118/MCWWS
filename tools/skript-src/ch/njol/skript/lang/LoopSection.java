/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.Debuggable;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SectionExitHandler;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.TriggerItem;
import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.event.Event;

public abstract class LoopSection
extends Section
implements SyntaxElement,
Debuggable,
SectionExitHandler {
    protected final transient Map<Event, Long> currentLoopCounter = new WeakHashMap<Event, Long>();

    public long getLoopCounter(Event event) {
        return this.currentLoopCounter.getOrDefault(event, 1L);
    }

    @Override
    public abstract TriggerItem getActualNext();

    @Override
    public void exit(Event event) {
        this.currentLoopCounter.remove(event);
    }
}

