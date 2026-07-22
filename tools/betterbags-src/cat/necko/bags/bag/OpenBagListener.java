/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 */
package cat.necko.bags.bag;

import cat.necko.bags.Plugin;
import cat.necko.bags.bag.data.PlayerData;
import cat.necko.bags.bag.inventory.BagInventory;
import cat.necko.bags.config.bags.BagsData;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class OpenBagListener
implements Listener {
    private final Plugin plugin;

    public OpenBagListener(Plugin plugin) {
        this.plugin = plugin;
    }

    private boolean openBag(ItemStack item, Player player) {
        if (!BagsData.BAG.compareTags(item)) {
            return false;
        }
        return this.openBag(player);
    }

    private boolean openBag(Player player) {
        if (BagInventory.isOpened(player)) {
            return false;
        }
        PlayerData playerData = this.plugin.getPlayerData(player.getUniqueId());
        new BagInventory(playerData).openFor(player);
        return true;
    }

    @EventHandler
    public void onBagRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        event.setCancelled(this.openBag(event.getItem(), event.getPlayer()));
    }

    @EventHandler
    public void onBagDrop(PlayerDropItemEvent event) {
        event.setCancelled(this.openBag(event.getItemDrop().getItemStack(), event.getPlayer()));
    }

    @EventHandler
    public void onBagClick(InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof PlayerInventory)) {
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        ItemStack item = event.getCurrentItem();
        if (!BagsData.BAG.compareTags(item)) {
            return;
        }
        if (this.plugin.getConfigData().bagMoveAbility) {
            return;
        }
        event.setCancelled(true);
        this.openBag(player);
        player.updateInventory();
    }
}

