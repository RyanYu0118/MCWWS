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
     * 短时关闭 UltimateShopStash 对「下一次商店买入」的溢出入库，避免刚买的一组进仓库而不是主手。
     */
    void suppressStashCollect(Player player, org.bukkit.Material material) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("MCWWS_UltimateShopStash")) {
            return;
        }
        try {
            org.bukkit.plugin.Plugin stash = plugin.getServer().getPluginManager().getPlugin("MCWWS_UltimateShopStash");
            if (stash == null) {
                return;
            }
            stash.getClass().getMethod("skipNextShopBuyDeposit", Player.class).invoke(stash, player);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("无法跳过仓库购买入库: " + ex.getMessage());
        }
    }

    /** 清掉未消费的一次性跳过标记（交易取消时用）。 */
    void clearStashBuySkip(Player player) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("MCWWS_UltimateShopStash")) {
            return;
        }
        try {
            org.bukkit.plugin.Plugin stash = plugin.getServer().getPluginManager().getPlugin("MCWWS_UltimateShopStash");
            if (stash == null) {
                return;
            }
            stash.getClass().getMethod("consumeSkipBuyDeposit", Player.class).invoke(stash, player);
        } catch (ReflectiveOperationException ignored) {
            // 旧版仓库没有该方法时忽略
        }
    }

    /**
     * 购买完成后，从身上取出最多 {@code amount} 个指定材料放进主手。
     * 若主手已是该材质则补足到目标数量；否则整组换上主手。
     */
    void equipPurchased(Player player, org.bukkit.Material material, int amount) {
        if (material == null || amount <= 0) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        int want = Math.min(amount, material.getMaxStackSize());

        ItemStack held = inventory.getItemInMainHand();
        if (held != null && held.getType() == material && held.getAmount() >= want) {
            return;
        }

        int alreadyHeld = 0;
        if (held != null && held.getType() == material) {
            alreadyHeld = held.getAmount();
            inventory.setItemInMainHand(null);
        }

        int need = want - alreadyHeld;
        int gathered = alreadyHeld;

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
            tryPullFromStash(player, material, need);
            held = inventory.getItemInMainHand();
            if (held != null && held.getType() == material && held.getAmount() > 0) {
                return;
            }
            return;
        }
        inventory.setItemInMainHand(new ItemStack(material, gathered));
    }

    /** 若购得物品已被仓库吸走，尝试取回并放到主手。 */
    private void tryPullFromStash(Player player, org.bukkit.Material material, int amount) {
        if (amount <= 0 || !plugin.getServer().getPluginManager().isPluginEnabled("MCWWS_UltimateShopStash")) {
            return;
        }
        try {
            org.bukkit.plugin.Plugin stash = plugin.getServer().getPluginManager().getPlugin("MCWWS_UltimateShopStash");
            if (stash == null) {
                return;
            }
            Object storage = stash.getClass().getMethod("storage").invoke(stash);
            if (storage == null) {
                return;
            }
            String key = material.name();
            Object haveObj = storage.getClass()
                    .getMethod("getAmount", java.util.UUID.class, String.class)
                    .invoke(storage, player.getUniqueId(), key);
            long have = haveObj instanceof Number n ? n.longValue() : 0L;
            if (have <= 0) {
                return;
            }
            long take = Math.min(amount, have);
            Object ok = storage.getClass()
                    .getMethod("tryRemove", java.util.UUID.class, String.class, long.class)
                    .invoke(storage, player.getUniqueId(), key, take);
            if (!Boolean.TRUE.equals(ok)) {
                return;
            }
            player.getInventory().setItemInMainHand(new ItemStack(material, (int) take));
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("无法从仓库取回选块购买物品: " + ex.getMessage());
        }
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
