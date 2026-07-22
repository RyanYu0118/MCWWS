/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerPortalEvent
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.plugin.EventExecutor
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.regions.events;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.hooks.regions.RegionsPlugin;
import ch.njol.skript.hooks.regions.classes.Region;
import ch.njol.skript.hooks.regions.events.RegionBorderEvent;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.registrations.EventValues;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.ScriptWarning;

public class EvtRegionBorder
extends SkriptEvent {
    private static final EventExecutor EXECUTOR;
    private static final List<Trigger> TRIGGERS;
    private static final AtomicBoolean REGISTERED_EXECUTORS;
    private boolean enter;
    @Nullable
    private Literal<Region> regions;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void callEvent(Region region, PlayerMoveEvent event, boolean enter) {
        RegionBorderEvent regionEvent = new RegionBorderEvent(region, event.getPlayer(), enter);
        regionEvent.setCancelled(event.isCancelled());
        List<Trigger> list = TRIGGERS;
        synchronized (list) {
            for (Trigger trigger : TRIGGERS) {
                if (!((EvtRegionBorder)trigger.getEvent()).applies(regionEvent)) continue;
                trigger.execute(regionEvent);
            }
        }
        event.setCancelled(regionEvent.isCancelled());
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.enter = parseResult.hasTag("enter");
        this.regions = args.length == 0 ? null : args[0];
        ScriptWarning.printDeprecationWarning("Skript's region syntaxes are deprecated and will be removed in a future release. For WorldGuard support, we recommend using skript-worldguard: https://github.com/SkriptLang/skript-worldguard");
        return true;
    }

    @Override
    public boolean postLoad() {
        TRIGGERS.add(this.trigger);
        if (REGISTERED_EXECUTORS.compareAndSet(false, true)) {
            EventPriority priority = SkriptConfig.defaultEventPriority.value();
            Bukkit.getPluginManager().registerEvent(PlayerMoveEvent.class, new Listener(this){}, priority, EXECUTOR, (Plugin)Skript.getInstance(), true);
            Bukkit.getPluginManager().registerEvent(PlayerTeleportEvent.class, new Listener(this){}, priority, EXECUTOR, (Plugin)Skript.getInstance(), true);
            Bukkit.getPluginManager().registerEvent(PlayerPortalEvent.class, new Listener(this){}, priority, EXECUTOR, (Plugin)Skript.getInstance(), true);
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
        return (this.enter ? "enter" : "leave") + " of " + (this.regions == null ? "a region" : this.regions.toString(event, debug));
    }

    private boolean applies(RegionBorderEvent event) {
        if (this.enter != event.isEntering()) {
            return false;
        }
        if (this.regions == null) {
            return true;
        }
        Region region = event.getRegion();
        return this.regions.check(event, r -> r.equals(region));
    }

    static {
        Skript.registerEvent("Region Enter/Leave", EvtRegionBorder.class, RegionBorderEvent.class, "(:enter[ing]|leav(e|ing)|exit[ing]) [of] ([a] region|[[the] region] %-regions%)", "region (:enter[ing]|leav(e|ing)|exit[ing])").description("Called when a player enters or leaves a <a href='#region'>region</a>.", "This event requires a supported regions plugin to be installed.").examples("on region exit:", "\tmessage \"Leaving %region%.\"").since("2.1").requiredPlugins("Supported regions plugin");
        EventValues.registerEventValue(RegionBorderEvent.class, Region.class, RegionBorderEvent::getRegion);
        EventValues.registerEventValue(RegionBorderEvent.class, Player.class, RegionBorderEvent::getPlayer);
        EXECUTOR = new EventExecutor(){
            @Nullable
            Event last = null;

            public void execute(@Nullable Listener listener, Event event) {
                Location from;
                if (event == this.last) {
                    return;
                }
                this.last = event;
                PlayerMoveEvent moveEvent = (PlayerMoveEvent)event;
                Location to = moveEvent.getTo();
                if (to.equals((Object)(from = moveEvent.getFrom()))) {
                    return;
                }
                Set<Region> oldRegions = RegionsPlugin.getRegionsAt(from);
                Set<Region> newRegions = RegionsPlugin.getRegionsAt(to);
                for (Region oldRegion : oldRegions) {
                    if (newRegions.contains(oldRegion)) continue;
                    EvtRegionBorder.callEvent(oldRegion, moveEvent, false);
                }
                for (Region newRegion : newRegions) {
                    if (oldRegions.contains(newRegion)) continue;
                    EvtRegionBorder.callEvent(newRegion, moveEvent, true);
                }
            }
        };
        TRIGGERS = Collections.synchronizedList(new ArrayList());
        REGISTERED_EXECUTORS = new AtomicBoolean();
    }
}

