/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.event.Event
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.plugin.EventExecutor
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.SkriptEventHandler;
import ch.njol.skript.aliases.ItemData;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.events.EvtPressurePlate;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.registrations.Classes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class EvtMoveOn
extends SkriptEvent {
    private static final Map<Material, List<Trigger>> ITEM_TYPE_TRIGGERS;
    private static final AtomicBoolean REGISTERED_EXECUTOR;
    private static final EventExecutor EXECUTOR;
    private ItemType[] types;

    @Nullable
    private static Block getOnBlock(Location location) {
        Block block = location.getWorld().getBlockAt(location.getBlockX(), (int)(Math.ceil(location.getY()) - 1.0), location.getBlockZ());
        if (block.getType() == Material.AIR && Math.abs(location.getY() - (double)location.getBlockY() - 0.5) < 1.0E-10 && !ItemUtils.isFence(block = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ()))) {
            return null;
        }
        return block;
    }

    private static int getBlockY(double y, Block block) {
        if (ItemUtils.isFence(block) && Math.abs(y - Math.floor(y) - 0.5) < 1.0E-10) {
            return (int)Math.floor(y) - 1;
        }
        return (int)Math.ceil(y) - 1;
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        Literal<?> types = args[0];
        if (types == null) {
            return false;
        }
        for (ItemType type : this.types = (ItemType[])types.getAll()) {
            if (type.isAll()) {
                Skript.error("Can't use an 'on walk' event with an alias that matches all blocks");
                return false;
            }
            for (ItemData data : type) {
                if (data.getType().isBlock() && !ItemUtils.isAir(data.getType())) continue;
                Skript.error(String.valueOf(type) + " is not a block and can thus not be walked on");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean postLoad() {
        HashSet<Material> materialSet = new HashSet<Material>();
        for (ItemType type : this.types) {
            for (ItemData data : type) {
                materialSet.add(data.getType());
            }
        }
        for (Material material : materialSet) {
            ITEM_TYPE_TRIGGERS.computeIfAbsent(material, k -> new ArrayList()).add(this.trigger);
        }
        if (REGISTERED_EXECUTOR.compareAndSet(false, true)) {
            Bukkit.getPluginManager().registerEvent(PlayerMoveEvent.class, new Listener(this){}, SkriptConfig.defaultEventPriority.value(), EXECUTOR, (Plugin)Skript.getInstance(), true);
        }
        return true;
    }

    @Override
    public void unload() {
        Iterator<Map.Entry<Material, List<Trigger>>> iterator = ITEM_TYPE_TRIGGERS.entrySet().iterator();
        while (iterator.hasNext()) {
            List<Trigger> triggers = iterator.next().getValue();
            triggers.remove(this.trigger);
            if (!triggers.isEmpty()) continue;
            iterator.remove();
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
        return "walk on " + Classes.toString(this.types, false);
    }

    static {
        new EvtPressurePlate();
        Skript.registerEvent("Move On", EvtMoveOn.class, PlayerMoveEvent.class, "(step|walk)[ing] (on|over) %*itemtypes%").description("Called when a player moves onto a certain type of block.", "Please note that using this event can cause lag if there are many players online.").examples("on walking on dirt or grass:", "on stepping on stone:").since("2.0");
        ITEM_TYPE_TRIGGERS = new ConcurrentHashMap<Material, List<Trigger>>();
        REGISTERED_EXECUTOR = new AtomicBoolean();
        EXECUTOR = (listener, e) -> {
            PlayerMoveEvent event = (PlayerMoveEvent)e;
            Location from = event.getFrom();
            Location to = event.getTo();
            if (!ITEM_TYPE_TRIGGERS.isEmpty()) {
                Block fromOnBlock;
                Block block = EvtMoveOn.getOnBlock(to);
                if (block == null || ItemUtils.isAir(block.getType())) {
                    return;
                }
                Material id = block.getType();
                List<Trigger> triggers = ITEM_TYPE_TRIGGERS.get(id);
                if (triggers == null) {
                    return;
                }
                int y = EvtMoveOn.getBlockY(to.getY(), block);
                if (to.getWorld().equals((Object)from.getWorld()) && to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ() && (fromOnBlock = EvtMoveOn.getOnBlock(from)) != null && y == EvtMoveOn.getBlockY(from.getY(), fromOnBlock) && fromOnBlock.getType() == id) {
                    return;
                }
                SkriptEventHandler.logEventStart((Event)event);
                block0: for (Trigger trigger : triggers) {
                    for (ItemType type : ((EvtMoveOn)trigger.getEvent()).types) {
                        if (!type.isOfType(block)) continue;
                        SkriptEventHandler.logTriggerStart(trigger);
                        trigger.execute((Event)event);
                        SkriptEventHandler.logTriggerEnd(trigger);
                        continue block0;
                    }
                }
                SkriptEventHandler.logEventEnd();
            }
        };
    }
}

