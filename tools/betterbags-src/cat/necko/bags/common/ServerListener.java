/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerAttemptPickupItemEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.persistence.PersistentDataType
 */
package cat.necko.bags.common;

import cat.necko.bags.Plugin;
import cat.necko.bags.bag.data.PlayerData;
import cat.necko.bags.config.bags.BagsData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ServerListener
implements Listener {
    private final Plugin plugin;

    public ServerListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!this.plugin.getConfigData().shouldGiveOnJoin) {
            return;
        }
        BagsData.updatePlayerBag(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!this.plugin.getConfigData().shouldGiveOnRespawn) {
            return;
        }
        BagsData.updatePlayerBag(event.getPlayer());
    }

    @EventHandler
    public void onItemPickup(PlayerAttemptPickupItemEvent event) {
        if (!this.plugin.getConfigData().autoPicking) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        if (this.plugin.getConfigData().pickupOnlySellable && !this.plugin.getItemsData().isSellable(item.getType())) {
            return;
        }
        if (item.getPersistentDataContainer().has(BagsData.ItemHash.KEY, PersistentDataType.STRING)) {
            item.setAmount(0);
            event.getItem().setItemStack(item);
            return;
        }
        PlayerData data = this.plugin.getPlayerData(player.getUniqueId());
        int returned = data.addItem(item);
        item.setAmount(returned);
        event.getItem().setItemStack(item);
        BagsData.updatePlayerBag(player);
    }
}

