/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event$Result
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ch.njol.skript.bukkitutil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ClickEventTracker {
    final Map<UUID, TrackedEvent> firstEvents = new HashMap<UUID, TrackedEvent>();
    private final Set<Cancellable> modifiedEvents = new HashSet<Cancellable>();

    public ClickEventTracker(JavaPlugin plugin) {
        Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)plugin, () -> {
            this.firstEvents.clear();
            this.modifiedEvents.clear();
        }, 1L, 1L);
    }

    public boolean checkEvent(Player player, Cancellable event, EquipmentSlot hand) {
        UUID uuid = player.getUniqueId();
        TrackedEvent first = this.firstEvents.get(uuid);
        if (first != null && first.event != event) {
            if (!this.modifiedEvents.contains(first.event)) {
                return false;
            }
            if (event instanceof PlayerInteractEvent) {
                PlayerInteractEvent current = (PlayerInteractEvent)event;
                Cancellable previous = first.event;
                if (previous instanceof PlayerInteractEvent) {
                    PlayerInteractEvent prevClick = (PlayerInteractEvent)previous;
                    current.setUseInteractedBlock(prevClick.useInteractedBlock());
                    current.setUseItemInHand(prevClick.useItemInHand());
                } else {
                    Event.Result newResult = previous.isCancelled() ? Event.Result.DENY : Event.Result.DEFAULT;
                    current.setUseInteractedBlock(newResult);
                    current.setUseItemInHand(newResult);
                }
            } else {
                event.setCancelled(first.event.isCancelled());
            }
            return false;
        }
        this.firstEvents.put(uuid, new TrackedEvent(event, hand));
        return true;
    }

    public void eventModified(Cancellable event) {
        this.modifiedEvents.add(event);
    }

    private static class TrackedEvent {
        final Cancellable event;
        final EquipmentSlot hand;

        public TrackedEvent(Cancellable event, EquipmentSlot hand) {
            this.event = event;
            this.hand = hand;
        }
    }
}

