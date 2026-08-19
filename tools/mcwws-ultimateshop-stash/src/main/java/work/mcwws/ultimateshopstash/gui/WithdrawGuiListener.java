package work.mcwws.ultimateshopstash.gui;

import cn.superiormc.ultimateshop.gui.inv.ShopGUI;
import cn.superiormc.ultimateshop.objects.ObjectShop;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.Chat;
import work.mcwws.ultimateshopstash.util.ItemKeys;
import work.mcwws.ultimateshopstash.util.Messages;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class WithdrawGuiListener implements Listener {

    private final McwwsUltimateShopStashPlugin plugin;

    public WithdrawGuiListener(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WithdrawHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        WithdrawSession session = holder.session();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (slot == WithdrawMenu.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == WithdrawMenu.SLOT_BACK) {
            player.closeInventory();
            reopenShop(player, session.shopName());
            return;
        }
        for (int i = 0; i < WithdrawMenu.AMOUNT_SLOTS.length; i++) {
            if (slot == WithdrawMenu.AMOUNT_SLOTS[i]) {
                session.selectedAmount(WithdrawMenu.AMOUNT_VALUES[i]);
                plugin.withdrawMenu().refreshConfirm(player, event.getInventory(), session);
                playClick(player);
                return;
            }
        }
        if (slot == WithdrawMenu.SLOT_CONFIRM) {
            withdraw(player, session);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof WithdrawHolder) {
            plugin.withdrawMenu().clear((Player) event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof WithdrawHolder) {
            event.setCancelled(true);
        }
    }

    private void withdraw(Player player, WithdrawSession session) {
        long stash = plugin.storage().getAmount(player.getUniqueId(), session.itemKey());
        int unitMultiplier = session.selectedAmount();
        int unitSize = ItemKeys.unitSize(session.objectItem(), player);
        long totalItems = (long) unitMultiplier * unitSize;
        totalItems = Math.min(totalItems, plugin.maxWithdrawAmount());
        totalItems = Math.min(totalItems, stash);
        if (totalItems <= 0) {
            Chat.send(player, plugin.messages(), "withdraw-empty", Map.of(
                    "item", Messages.displayMaterial(session.itemKey())
            ));
            return;
        }
        Material material = Material.matchMaterial(session.itemKey().toUpperCase(Locale.ROOT));
        if (material == null) {
            Chat.send(player, plugin.messages(), "not-shop-item", null);
            return;
        }
        long remain = totalItems;
        while (remain > 0) {
            int stackSize = (int) Math.min(remain, material.getMaxStackSize());
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(material, stackSize));
            if (!leftover.isEmpty()) {
                Chat.send(player, plugin.messages(), "withdraw-no-space", null);
                return;
            }
            remain -= stackSize;
        }
        plugin.storage().remove(player.getUniqueId(), session.itemKey(), totalItems);
        Chat.send(player, plugin.messages(), "withdraw-success", Map.of(
                "amount", String.valueOf(totalItems),
                "item", Messages.displayMaterial(session.itemKey())
        ));
        String soundName = plugin.getConfig().getString("sounds.withdraw", "ENTITY_ITEM_PICKUP");
        try {
            Sound sound = org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(
                    soundName.toLowerCase(Locale.ROOT).replace('.', '_')));
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 0.6f, 1f);
            }
        } catch (Throwable ignored) {
            // ignore
        }
        plugin.withdrawMenu().paint(player, player.getOpenInventory().getTopInventory(), session);
        plugin.lorePatcher().patchOpenShop(player);
    }

    private void reopenShop(Player player, String shopName) {
        ObjectShop shop = ItemKeys.findShop(shopName);
        if (shop == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> ShopGUI.openGUI(player, shop, false, false));
    }

    private void playClick(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }
}
