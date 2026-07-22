/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ArrayListMultimap
 *  com.google.common.collect.Multimap
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.Event$Result
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.plugin.EventExecutor
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.RegisteredListener
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.timings.SkriptTimings;
import ch.njol.skript.util.Task;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.Nullable;

public final class SkriptEventHandler {
    private static final PriorityListener[] listeners;
    private static final Multimap<Class<? extends Event>, Trigger> triggers;
    private static long startEvent;
    private static long startTrigger;
    @Deprecated(since="2.9.0", forRemoval=true)
    public static final Set<Class<? extends Event>> listenCancelled;
    private static final Map<Class<? extends Event>, Method> handlerListMethods;
    private static final Map<Method, WeakReference<HandlerList>> handlerListCache;

    private SkriptEventHandler() {
    }

    private static List<Trigger> getTriggers(Class<? extends Event> event) {
        HandlerList eventHandlerList = SkriptEventHandler.getHandlerList(event);
        assert (eventHandlerList != null);
        return triggers.asMap().entrySet().stream().filter(entry -> ((Class)entry.getKey()).isAssignableFrom(event) && SkriptEventHandler.getHandlerList((Class)entry.getKey()) == eventHandlerList).flatMap(entry -> ((Collection)entry.getValue()).stream()).distinct().collect(Collectors.toList());
    }

    private static void check(Event event, EventPriority priority) {
        List<Trigger> triggers = SkriptEventHandler.getTriggers(event.getClass());
        if (triggers.isEmpty()) {
            return;
        }
        boolean isCancelled = SkriptEventHandler.isCancelled(event);
        SkriptEventHandler.logEventStart(event, priority);
        for (Trigger trigger : triggers) {
            SkriptEvent triggerEvent = trigger.getEvent();
            if (triggerEvent.getEventPriority() != priority || !triggerEvent.getListeningBehavior().matches(isCancelled)) continue;
            SkriptEventHandler.execute(trigger, event);
        }
        SkriptEventHandler.logEventEnd();
    }

    private static boolean isCancelled(Event event) {
        if (!(event instanceof Cancellable)) {
            return false;
        }
        Cancellable cancellable = (Cancellable)event;
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent interactEvent = (PlayerInteractEvent)event;
            return SkriptEventHandler.isPlayerInteractEventCancelled(interactEvent);
        }
        return cancellable.isCancelled();
    }

    private static boolean isPlayerInteractEventCancelled(PlayerInteractEvent event) {
        return switch (event.getAction()) {
            case Action.LEFT_CLICK_AIR, Action.RIGHT_CLICK_AIR -> {
                if (event.useItemInHand() == Event.Result.DENY) {
                    yield true;
                }
                yield false;
            }
            case Action.RIGHT_CLICK_BLOCK -> {
                if (event.useItemInHand() == Event.Result.DENY && event.useInteractedBlock() == Event.Result.DENY) {
                    yield true;
                }
                yield false;
            }
            case Action.PHYSICAL -> {
                if (event.useInteractedBlock() == Event.Result.DENY) {
                    yield true;
                }
                yield false;
            }
            default -> event.useItemInHand() == Event.Result.DENY || event.useInteractedBlock() == Event.Result.DENY;
        };
    }

    private static void execute(Trigger trigger, Event event) {
        Runnable execute = () -> {
            SkriptEventHandler.logTriggerStart(trigger);
            Object timing = SkriptTimings.start(trigger.getDebugLabel());
            trigger.execute(event);
            SkriptTimings.stop(timing);
            SkriptEventHandler.logTriggerEnd(trigger);
        };
        if (trigger.getEvent().canExecuteAsynchronously()) {
            if (trigger.getEvent().check(event)) {
                execute.run();
            }
        } else {
            Task.callSync(() -> {
                if (trigger.getEvent().check(event)) {
                    execute.run();
                }
                return null;
            });
        }
    }

    public static void logEventStart(Event event) {
        SkriptEventHandler.logEventStart(event, null);
    }

    public static void logEventStart(Event event, @Nullable EventPriority priority) {
        startEvent = System.nanoTime();
        if (!Skript.logVeryHigh()) {
            return;
        }
        Skript.info("");
        String message = "== " + event.getClass().getName();
        if (priority != null) {
            message = message + " with priority " + String.valueOf(priority);
        }
        if (event instanceof Cancellable && ((Cancellable)event).isCancelled()) {
            message = message + " (cancelled)";
        }
        Skript.info(message + " ==");
    }

    public static void logEventEnd() {
        if (!Skript.logVeryHigh()) {
            return;
        }
        Skript.info("== took " + 1.0 * (double)(System.nanoTime() - startEvent) / 1000000.0 + " milliseconds ==");
    }

    public static void logTriggerStart(Trigger trigger) {
        startTrigger = System.nanoTime();
        if (!Skript.logVeryHigh()) {
            return;
        }
        Skript.info("# " + trigger.getName());
    }

    public static void logTriggerEnd(Trigger t) {
        if (!Skript.logVeryHigh()) {
            return;
        }
        Skript.info("# " + t.getName() + " took " + 1.0 * (double)(System.nanoTime() - startTrigger) / 1000000.0 + " milliseconds");
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    public static void addSelfRegisteringTrigger(Trigger t) {
    }

    public static void registerBukkitEvents(Trigger trigger, Class<? extends Event>[] events) {
        for (Class<? extends Event> event : events) {
            SkriptEventHandler.registerBukkitEvent(trigger, event);
        }
    }

    public static void registerBukkitEvent(Trigger trigger, Class<? extends Event> event) {
        HandlerList handlerList = SkriptEventHandler.getHandlerList(event);
        if (handlerList == null) {
            return;
        }
        triggers.put(event, (Object)trigger);
        EventPriority priority = trigger.getEvent().getEventPriority();
        if (!SkriptEventHandler.isEventRegistered(handlerList, priority)) {
            PriorityListener listener = listeners[priority.ordinal()];
            Bukkit.getPluginManager().registerEvent(event, (Listener)listener, priority, listener.executor, (Plugin)Skript.getInstance());
        }
    }

    public static void unregisterBukkitEvents(Trigger trigger) {
        Iterator entryIterator = triggers.entries().iterator();
        block0: while (entryIterator.hasNext()) {
            Map.Entry entry = (Map.Entry)entryIterator.next();
            if (entry.getValue() != trigger) continue;
            Class event = (Class)entry.getKey();
            entryIterator.remove();
            EventPriority priority = trigger.getEvent().getEventPriority();
            for (Trigger eventTrigger : triggers.get((Object)event)) {
                if (eventTrigger.getEvent().getEventPriority() != priority) continue;
                continue block0;
            }
            HandlerList handlerList = SkriptEventHandler.getHandlerList(event);
            if (handlerList == null) continue;
            Skript skript = Skript.getInstance();
            for (RegisteredListener registeredListener : handlerList.getRegisteredListeners()) {
                Listener listener = registeredListener.getListener();
                if (registeredListener.getPlugin() != skript || !(listener instanceof PriorityListener) || ((PriorityListener)listener).priority != priority) continue;
                handlerList.unregister(listener);
            }
        }
    }

    @Nullable
    private static HandlerList getHandlerList(Class<? extends Event> eventClass) {
        try {
            HandlerList handlerList;
            Method method = SkriptEventHandler.getHandlerListMethod(eventClass);
            WeakReference<HandlerList> handlerListReference = handlerListCache.get(method);
            HandlerList handlerList2 = handlerList = handlerListReference != null ? (HandlerList)handlerListReference.get() : null;
            if (handlerList == null) {
                method.setAccessible(true);
                handlerList = (HandlerList)method.invoke(null, new Object[0]);
                handlerListCache.put(method, new WeakReference<HandlerList>(handlerList));
            }
            return handlerList;
        }
        catch (Exception ex) {
            Skript.exception((Throwable)ex, "Failed to get HandlerList for event " + eventClass.getName());
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static Method getHandlerListMethod(Class<? extends Event> eventClass) {
        Method method;
        Map<Class<? extends Event>, Method> map = handlerListMethods;
        synchronized (map) {
            method = handlerListMethods.get(eventClass);
            if (method == null) {
                method = SkriptEventHandler.getHandlerListMethod_i(eventClass);
                if (method != null) {
                    method.setAccessible(true);
                }
                handlerListMethods.put(eventClass, method);
            }
        }
        if (method == null) {
            throw new RuntimeException("No getHandlerList method found");
        }
        return method;
    }

    @Nullable
    private static Method getHandlerListMethod_i(Class<? extends Event> eventClass) {
        try {
            return eventClass.getDeclaredMethod("getHandlerList", new Class[0]);
        }
        catch (NoSuchMethodException e) {
            if (eventClass.getSuperclass() != null && !eventClass.getSuperclass().equals(Event.class) && Event.class.isAssignableFrom(eventClass.getSuperclass())) {
                return SkriptEventHandler.getHandlerListMethod(eventClass.getSuperclass().asSubclass(Event.class));
            }
            return null;
        }
    }

    private static boolean isEventRegistered(HandlerList handlerList, EventPriority priority) {
        for (RegisteredListener registeredListener : handlerList.getRegisteredListeners()) {
            Listener listener = registeredListener.getListener();
            if (registeredListener.getPlugin() != Skript.getInstance() || !(listener instanceof PriorityListener) || ((PriorityListener)listener).priority != priority) continue;
            return true;
        }
        return false;
    }

    static {
        EventPriority[] priorities = EventPriority.values();
        listeners = new PriorityListener[priorities.length];
        for (int i = 0; i < priorities.length; ++i) {
            SkriptEventHandler.listeners[i] = new PriorityListener(priorities[i]);
        }
        triggers = ArrayListMultimap.create();
        listenCancelled = new HashSet<Class<? extends Event>>();
        handlerListMethods = new HashMap<Class<? extends Event>, Method>();
        handlerListCache = new HashMap<Method, WeakReference<HandlerList>>();
    }

    public static class PriorityListener
    implements Listener {
        public final EventPriority priority;
        public final EventExecutor executor = (listener, event) -> SkriptEventHandler.check(event, ((PriorityListener)listener).priority);

        public PriorityListener(EventPriority priority) {
            this.priority = priority;
        }
    }
}

