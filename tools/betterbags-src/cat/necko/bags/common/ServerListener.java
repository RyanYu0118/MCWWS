/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.player.PlayerAttemptPickupItemEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.inventory.ItemStack
 */
package cat.necko.bags.common;

import cat.necko.bags.Plugin;
import cat.necko.bags.config.bags.BagsData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class ServerListener
implements Listener {
    private final Plugin plugin;

    public ServerListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        BagsData.stripBagItems(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        BagsData.stripBagItems(event.getPlayer());
    }

    @EventHandler
    public void onItemPickup(PlayerAttemptPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (BagsData.isBagItem(item)) {
            event.setCancelled(true);
            item.setAmount(0);
            event.getItem().setItemStack(item);
            event.getItem().remove();
        }
    }

    @EventHandler
    public void onBagHeadPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (BagsData.isBagItem(event.getItemInHand())) {
            event.setCancelled(true);
            BagsData.stripBagItems(player);
        }
    }
}
