/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockExpEvent
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.event.entity.ExpBottleEvent
 *  org.bukkit.event.player.PlayerFishEvent
 *  org.bukkit.event.player.PlayerFishEvent$State
 *  org.bukkit.plugin.EventExecutor
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.SkriptEventHandler;
import ch.njol.skript.events.bukkit.ExperienceSpawnEvent;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Experience;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class EvtExperienceSpawn
extends SkriptEvent {
    private static final List<Trigger> TRIGGERS;
    private static final AtomicBoolean REGISTERED_EXECUTORS;
    private static final EventExecutor EXECUTOR;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean postLoad() {
        TRIGGERS.add(this.trigger);
        if (REGISTERED_EXECUTORS.compareAndSet(false, true)) {
            EventPriority priority = SkriptConfig.defaultEventPriority.value();
            for (Class clazz : new Class[]{BlockExpEvent.class, EntityDeathEvent.class, ExpBottleEvent.class, PlayerFishEvent.class}) {
                Bukkit.getPluginManager().registerEvent(clazz, new Listener(this){}, priority, EXECUTOR, (Plugin)Skript.getInstance(), true);
            }
        }
        return true;
    }

    @Override
    public void unload() {
        TRIGGERS.remove(this.trigger);
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
        return "experience spawn";
    }

    static {
        Skript.registerEvent("Experience Spawn", EvtExperienceSpawn.class, ExperienceSpawnEvent.class, "[e]xp[erience] [orb] spawn", "spawn of [a[n]] [e]xp[erience] [orb]").description("Called whenever experience is about to spawn.", "Please note that this event will not fire for xp orbs spawned by plugins (including Skript) with Bukkit.").examples("on xp spawn:", "\tworld is \"minigame_world\"", "\tcancel event").since("2.0");
        EventValues.registerEventValue(ExperienceSpawnEvent.class, Location.class, ExperienceSpawnEvent::getLocation);
        EventValues.registerEventValue(ExperienceSpawnEvent.class, Experience.class, event -> new Experience(event.getSpawnedXP()));
        TRIGGERS = Collections.synchronizedList(new ArrayList());
        REGISTERED_EXECUTORS = new AtomicBoolean();
        EXECUTOR = (listener, event) -> {
            ExperienceSpawnEvent experienceEvent;
            if (event instanceof BlockExpEvent) {
                experienceEvent = new ExperienceSpawnEvent(((BlockExpEvent)event).getExpToDrop(), ((BlockExpEvent)event).getBlock().getLocation().add(0.5, 0.5, 0.5));
            } else if (event instanceof EntityDeathEvent) {
                experienceEvent = new ExperienceSpawnEvent(((EntityDeathEvent)event).getDroppedExp(), ((EntityDeathEvent)event).getEntity().getLocation());
            } else if (event instanceof ExpBottleEvent) {
                experienceEvent = new ExperienceSpawnEvent(((ExpBottleEvent)event).getExperience(), ((ExpBottleEvent)event).getEntity().getLocation());
            } else if (event instanceof PlayerFishEvent) {
                if (((PlayerFishEvent)event).getState() != PlayerFishEvent.State.CAUGHT_FISH) {
                    return;
                }
                experienceEvent = new ExperienceSpawnEvent(((PlayerFishEvent)event).getExpToDrop(), ((PlayerFishEvent)event).getPlayer().getLocation());
            } else {
                assert (false);
                return;
            }
            SkriptEventHandler.logEventStart(event);
            List<Trigger> list = TRIGGERS;
            synchronized (list) {
                for (Trigger trigger : TRIGGERS) {
                    SkriptEventHandler.logTriggerStart(trigger);
                    trigger.execute(experienceEvent);
                    SkriptEventHandler.logTriggerEnd(trigger);
                }
            }
            SkriptEventHandler.logEventEnd();
            if (experienceEvent.isCancelled()) {
                experienceEvent.setSpawnedXP(0);
            }
            if (event instanceof BlockExpEvent) {
                ((BlockExpEvent)event).setExpToDrop(experienceEvent.getSpawnedXP());
            } else if (event instanceof EntityDeathEvent) {
                ((EntityDeathEvent)event).setDroppedExp(experienceEvent.getSpawnedXP());
            } else if (event instanceof ExpBottleEvent) {
                ((ExpBottleEvent)event).setExperience(experienceEvent.getSpawnedXP());
            } else if (event instanceof PlayerFishEvent) {
                ((PlayerFishEvent)event).setExpToDrop(experienceEvent.getSpawnedXP());
            }
        };
    }
}

