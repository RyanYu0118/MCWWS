package work.mcwws.ultimateshopstash.returning;

import cat.necko.bags.bag.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;

import java.io.IOException;
import java.util.Locale;

final class ItemReturnService {

    private static final int[] HOTBAR_SLOTS = range(0, 9);
    private static final int[] INVENTORY_SLOTS = range(9, 36);

    private final McwwsUltimateShopStashPlugin plugin;

    ItemReturnService(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    ReturnResult returnItems(Player player, String itemKey, long amount) {
        Material material = Material.matchMaterial(itemKey.toUpperCase(Locale.ROOT));
        if (material == null || amount <= 0) {
            return ReturnResult.failed();
        }

        ItemStack prototype = new ItemStack(material);
        PlayerInventory inventory = player.getInventory();
        long hotbarCapacity = capacity(inventory, HOTBAR_SLOTS, prototype);
        long inventoryCapacity = capacity(inventory, INVENTORY_SLOTS, prototype);

        long hotbarAmount = Math.min(amount, hotbarCapacity);
        long afterHotbar = amount - hotbarAmount;
        long inventoryAmount = Math.min(afterHotbar, inventoryCapacity);
        long betterBagsAmount = afterHotbar - inventoryAmount;

        BetterBagsTarget bags = betterBags(player);
        long bagsCapacity = bags == null ? 0 : bags.freeCapacity();
        if (betterBagsAmount > bagsCapacity) {
            return ReturnResult.failed();
        }

        if (betterBagsAmount > 0 && !addToBetterBags(bags, material, betterBagsAmount)) {
            return ReturnResult.failed();
        }

        ItemStack[] snapshot = cloneContents(inventory.getStorageContents());
        long hotbarLeft = addToSlots(inventory, HOTBAR_SLOTS, prototype, hotbarAmount);
        long inventoryLeft = addToSlots(inventory, INVENTORY_SLOTS, prototype, inventoryAmount);
        if (hotbarLeft != 0 || inventoryLeft != 0) {
            inventory.setStorageContents(snapshot);
            if (betterBagsAmount > 0) {
                removeFromBetterBags(bags, material, betterBagsAmount);
            }
            return ReturnResult.failed();
        }

        return new ReturnResult(true, hotbarAmount, inventoryAmount, betterBagsAmount);
    }

    private BetterBagsTarget betterBags(Player player) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("BetterBags")) {
            return null;
        }
        cat.necko.bags.Plugin bagsPlugin = cat.necko.bags.Plugin.getInstance();
        if (bagsPlugin == null) {
            return null;
        }
        PlayerData data = bagsPlugin.getPlayerData(player.getUniqueId());
        if (data == null || data.getLevel() == null) {
            return null;
        }
        return new BetterBagsTarget(
                bagsPlugin,
                data,
                Math.max(0L, (long) data.getLevel().capacity() - data.getItemsAmount())
        );
    }

    private boolean addToBetterBags(BetterBagsTarget target, Material material, long amount) {
        long added = 0;
        while (added < amount) {
            int stackAmount = (int) Math.min(material.getMaxStackSize(), amount - added);
            int leftover = target.data().addItem(new ItemStack(material, stackAmount));
            int accepted = stackAmount - Math.max(0, leftover);
            added += accepted;
            if (leftover > 0) {
                removeFromBetterBags(target, material, added);
                return false;
            }
        }
        saveBetterBags(target);
        return true;
    }

    private void removeFromBetterBags(BetterBagsTarget target, Material material, long amount) {
        long removed = 0;
        while (removed < amount) {
            int stackAmount = (int) Math.min(material.getMaxStackSize(), amount - removed);
            if (!target.data().removeItem(new ItemStack(material, stackAmount))) {
                plugin.getLogger().severe("回滚 BetterBags 物品失败: " + material + " x" + (amount - removed));
                return;
            }
            removed += stackAmount;
        }
        saveBetterBags(target);
    }

    private void saveBetterBags(BetterBagsTarget target) {
        try {
            target.data().save(target.plugin());
        } catch (IOException exception) {
            plugin.getLogger().warning("保存 BetterBags 失败，将由其定时保存任务重试: " + exception.getMessage());
        }
    }

    private static long capacity(PlayerInventory inventory, int[] slots, ItemStack prototype) {
        long capacity = 0;
        int maxStack = prototype.getMaxStackSize();
        for (int slot : slots) {
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType().isAir()) {
                capacity += maxStack;
            } else if (current.isSimilar(prototype)) {
                capacity += Math.max(0, maxStack - current.getAmount());
            }
        }
        return capacity;
    }

    private static long addToSlots(
            PlayerInventory inventory, int[] slots, ItemStack prototype, long amount) {
        long left = amount;
        int maxStack = prototype.getMaxStackSize();

        for (int slot : slots) {
            if (left <= 0) {
                break;
            }
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType().isAir() || !current.isSimilar(prototype)) {
                continue;
            }
            int add = (int) Math.min(left, Math.max(0, maxStack - current.getAmount()));
            if (add > 0) {
                ItemStack updated = current.clone();
                updated.setAmount(current.getAmount() + add);
                inventory.setItem(slot, updated);
                left -= add;
            }
        }

        for (int slot : slots) {
            if (left <= 0) {
                break;
            }
            ItemStack current = inventory.getItem(slot);
            if (current != null && !current.getType().isAir()) {
                continue;
            }
            int add = (int) Math.min(left, maxStack);
            ItemStack inserted = prototype.clone();
            inserted.setAmount(add);
            inventory.setItem(slot, inserted);
            left -= add;
        }
        return left;
    }

    private static ItemStack[] cloneContents(ItemStack[] source) {
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            clone[i] = source[i] == null ? null : source[i].clone();
        }
        return clone;
    }

    private static int[] range(int startInclusive, int endExclusive) {
        int[] result = new int[endExclusive - startInclusive];
        for (int i = 0; i < result.length; i++) {
            result[i] = startInclusive + i;
        }
        return result;
    }

    record ReturnResult(
            boolean success, long hotbarAmount, long inventoryAmount, long betterBagsAmount) {

        static ReturnResult failed() {
            return new ReturnResult(false, 0, 0, 0);
        }
    }

    private record BetterBagsTarget(
            cat.necko.bags.Plugin plugin, PlayerData data, long freeCapacity) {
    }
}
