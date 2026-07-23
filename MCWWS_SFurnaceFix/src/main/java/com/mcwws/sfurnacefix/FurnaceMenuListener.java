package com.mcwws.sfurnacefix;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

public final class FurnaceMenuListener implements Listener {

    private final Plugin plugin;

    public FurnaceMenuListener(Plugin plugin) {
        this.plugin = plugin;
    }

    private static boolean isBlockMenuFurnaceMachine(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        Block block = location.getBlock();
        Material type = block.getType();
        if (type != Material.FURNACE && type != Material.BLAST_FURNACE && type != Material.SMOKER) {
            return false;
        }

        if (!StorageCacheUtils.hasSlimefunBlock(location)) {
            return false;
        }

        SlimefunItem item = StorageCacheUtils.getSlimefunItem(location);
        if (item == null) {
            return false;
        }

        return BlockMenuPreset.isInventory(item.getId());
    }

    private static void openBlockMenu(Player player, Block block) {
        if (player == null || block == null) {
            return;
        }

        SlimefunBlockData data = StorageCacheUtils.getBlock(block.getLocation());
        if (data == null || !data.isDataLoaded()) {
            return;
        }

        DirtyChestMenu menu = data.getBlockMenu();
        if (menu == null) {
            return;
        }

        if (player.hasPermission("slimefun.inventory.bypass") || menu.canOpen(block, player)) {
            menu.open(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !isBlockMenuFurnaceMachine(block.getLocation())) {
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof Furnace furnace)) {
            return;
        }

        Location location = furnace.getLocation();
        if (!isBlockMenuFurnaceMachine(location)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        Block block = location.getBlock();
        Bukkit.getScheduler().runTask(plugin, () -> openBlockMenu(player, block));
    }
}
