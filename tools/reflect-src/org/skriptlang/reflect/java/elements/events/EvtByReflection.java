/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.lang.Literal
 *  ch.njol.skript.lang.SkriptEvent
 *  ch.njol.skript.lang.SkriptEvent$ListeningBehavior
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Trigger
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventException
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.EventExecutor
 *  org.bukkit.plugin.Plugin
 */
package org.skriptlang.reflect.java.elements.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.WrappedEvent;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

public class EvtByReflection
extends SkriptEvent {
    private Class<? extends Event>[] classes;
    private Listener listener;

    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        JavaType[] javaTypes = (JavaType[])args[0].getArray();
        this.classes = new Class[javaTypes.length];
        for (int i = 0; i < javaTypes.length; ++i) {
            JavaType javaType = javaTypes[i];
            Class<?> clazz = javaType.getJavaClass();
            if (!Event.class.isAssignableFrom(clazz)) {
                Skript.error((String)(clazz.getSimpleName() + " is not a Bukkit event"));
                return false;
            }
            this.classes[i] = clazz;
        }
        this.listener = new Listener(this){};
        return true;
    }

    public boolean check(Event event) {
        throw new UnsupportedOperationException();
    }

    public boolean postLoad() {
        for (Class<? extends Event> eventClass : this.classes) {
            MyEventExecutor executor = new MyEventExecutor(eventClass, this.listeningBehavior, this.trigger);
            Bukkit.getPluginManager().registerEvent(eventClass, this.listener, this.getEventPriority(), (EventExecutor)executor, (Plugin)SkriptMirror.getInstance(), this.listeningBehavior == SkriptEvent.ListeningBehavior.UNCANCELLED);
        }
        return true;
    }

    public void unload() {
        HandlerList.unregisterAll((Listener)this.listener);
    }

    public boolean isListeningBehaviorSupported() {
        return true;
    }

    public Class<? extends Event>[] getEventClasses() {
        boolean hasUncancellable = false;
        boolean hasCancellable = false;
        if (this.classes == null) {
            return new Class[]{BukkitEvent.class};
        }
        for (Class<? extends Event> eventClass : this.classes) {
            if (Cancellable.class.isAssignableFrom(eventClass)) {
                hasCancellable = true;
                continue;
            }
            hasUncancellable = true;
        }
        if (hasCancellable && hasUncancellable) {
            return new Class[]{BukkitEvent.class, CancellableBukkitEvent.class};
        }
        if (hasCancellable) {
            return new Class[]{CancellableBukkitEvent.class};
        }
        return new Class[]{BukkitEvent.class};
    }

    public String toString(Event e, boolean debug) {
        return Arrays.stream(this.classes).map(Class::getSimpleName).collect(Collectors.joining(", "));
    }

    static {
        Skript.registerEvent((String)"*reflection", EvtByReflection.class, BukkitEvent.class, (String[])new String[]{"%javatypes%"});
    }

    private static class MyEventExecutor
    implements EventExecutor {
        private final Class<? extends Event> eventClass;
        private final SkriptEvent.ListeningBehavior listeningBehavior;
        private final Trigger trigger;

        public MyEventExecutor(Class<? extends Event> eventClass, Trigger trigger) {
            this.eventClass = eventClass;
            this.listeningBehavior = SkriptEvent.ListeningBehavior.UNCANCELLED;
            this.trigger = trigger;
        }

        public MyEventExecutor(Class<? extends Event> eventClass, SkriptEvent.ListeningBehavior listeningBehavior, Trigger trigger) {
            this.eventClass = eventClass;
            this.listeningBehavior = listeningBehavior;
            this.trigger = trigger;
        }

        public void execute(Listener listener, Event event) throws EventException {
            if (this.eventClass.isInstance(event)) {
                if (event instanceof Cancellable && this.listeningBehavior != null && !this.listeningBehavior.matches(((Cancellable)event).isCancelled())) {
                    return;
                }
                BukkitEvent scriptEvent = event instanceof Cancellable ? new CancellableBukkitEvent((Cancellable)event) : new BukkitEvent(event);
                this.trigger.execute((Event)scriptEvent);
            }
        }
    }

    private static class BukkitEvent
    extends WrappedEvent {
        public BukkitEvent(Event event) {
            super(event, event.isAsynchronous());
        }

        public HandlerList getHandlers() {
            throw new IllegalStateException();
        }
    }

    private static class CancellableBukkitEvent
    extends BukkitEvent
    implements Cancellable {
        public CancellableBukkitEvent(Cancellable event) {
            super((Event)event);
        }

        public boolean isCancelled() {
            Event event = this.getDirectEvent();
            return ((Cancellable)event).isCancelled();
        }

        public void setCancelled(boolean cancel) {
            Event event = this.getDirectEvent();
            ((Cancellable)event).setCancelled(cancel);
        }
    }
}

