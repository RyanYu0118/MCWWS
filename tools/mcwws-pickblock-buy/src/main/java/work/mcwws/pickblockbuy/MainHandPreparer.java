package work.mcwws.pickblockbuy;

import cat.necko.bags.bag.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

/**
 * 购买前腾出主手：优先挪到身上空位，其次 BetterBags；均失败则拒绝购买。
 */
final class MainHandPreparer {

    enum Outcome {
        READY,
        MOVED_TO_INVENTORY,
        MOVED_TO_BAGS,
        FAILED_NO_SPACE
    }

    private final JavaPlugin plugin;

    MainHandPreparer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    Outcome prepare(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack held = inventory.getItemInMainHand();
        if (held == null || held.getType().isAir() || held.getAmount() <= 0) {
            return Outcome.READY;
        }

        ItemStack toMove = held.clone();
        int emptySlot = findEmptyStorageSlot(inventory, inventory.getHeldItemSlot());
        if (emptySlot >= 0) {
            inventory.setItem(emptySlot, toMove);
            inventory.setItemInMainHand(null);
            return Outcome.MOVED_TO_INVENTORY;
        }

        if (tryAddToBetterBags(player, toMove)) {
            inventory.setItemInMainHand(null);
            return Outcome.MOVED_TO_BAGS;
        }

        return Outcome.FAILED_NO_SPACE;
    }

    /**
     * 购买完成后，从身上取出最多 {@code amount} 个指定材料放进主手。
     */
    void equipPurchased(Player player, org.bukkit.Material material, int amount) {
        if (material == null || amount <= 0) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        int want = Math.min(amount, material.getMaxStackSize());
        int gathered = 0;

        ItemStack[] storage = inventory.getStorageContents();
        for (int i = 0; i < storage.length && gathered < want; i++) {
            ItemStack stack = storage[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }
            int take = Math.min(want - gathered, stack.getAmount());
            gathered += take;
            if (take >= stack.getAmount()) {
                storage[i] = null;
            } else {
                ItemStack reduced = stack.clone();
                reduced.setAmount(stack.getAmount() - take);
                storage[i] = reduced;
            }
        }
        inventory.setStorageContents(storage);

        if (gathered <= 0) {
            return;
        }
        inventory.setItemInMainHand(new ItemStack(material, gathered));
    }

    private static int findEmptyStorageSlot(PlayerInventory inventory, int excludeSlot) {
        ItemStack[] storage = inventory.getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            if (i == excludeSlot) {
                continue;
            }
            ItemStack stack = storage[i];
            if (stack == null || stack.getType().isAir()) {
                return i;
            }
        }
        return -1;
    }

    private boolean tryAddToBetterBags(Player player, ItemStack item) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("BetterBags")) {
            return false;
        }
        try {
            cat.necko.bags.Plugin bagsPlugin = cat.necko.bags.Plugin.getInstance();
            if (bagsPlugin == null) {
                return false;
            }
            PlayerData data = bagsPlugin.getPlayerData(player.getUniqueId());
            if (data == null || data.getLevel() == null) {
                return false;
            }
            long free = Math.max(0L, (long) data.getLevel().capacity() - data.getItemsAmount());
            if (free < item.getAmount()) {
                return false;
            }
            ItemStack toAdd = item.clone();
            int leftover = data.addItem(toAdd);
            if (leftover > 0) {
                // 部分失败：尽量回滚已吸入部分
                int accepted = item.getAmount() - leftover;
                if (accepted > 0) {
                    ItemStack rollback = item.clone();
                    rollback.setAmount(accepted);
                    data.removeItem(rollback);
                }
                return false;
            }
            try {
                data.save(bagsPlugin);
            } catch (IOException ex) {
                plugin.getLogger().warning("保存 BetterBags 失败: " + ex.getMessage());
            }
            return true;
        } catch (NoClassDefFoundError | Exception ex) {
            plugin.getLogger().warning("写入 BetterBags 失败: " + ex.getMessage());
            return false;
        }
    }
}
