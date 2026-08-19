package work.mcwws.ultimateshopstash.collect;

import cn.superiormc.ultimateshop.api.ItemFinishTransactionEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.notify.StashNotifier;
import work.mcwws.ultimateshopstash.util.ItemKeys;

public final class ShopBuyDepositListener implements Listener {

    private final McwwsUltimateShopStashPlugin plugin;

    public ShopBuyDepositListener(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBuy(ItemFinishTransactionEvent event) {
        if (!plugin.depositShopBuys() || !event.isBuyOrSell()) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.hasAutoCollect(player)) {
            return;
        }
        String key = ItemKeys.fromObjectItem(event.getItem());
        if (key == null || !plugin.catalog().contains(key)) {
            return;
        }
        if (plugin.exemptManager().isExempt(player, key)) {
            return;
        }
        int units = Math.max(1, event.getAmount());
        int unitSize = ItemKeys.unitSize(event.getItem(), player);
        long totalItems = (long) units * unitSize;
        Material material = Material.matchMaterial(key.toUpperCase(java.util.Locale.ROOT));
        if (material == null) {
            return;
        }
        long removed = removeMatching(player.getInventory(), material, totalItems);
        if (removed <= 0) {
            return;
        }
        plugin.storage().add(player.getUniqueId(), key, removed);
        StashNotifier.collect(plugin, player, key, removed);
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.lorePatcher().patchOpenShop(player));
    }

    private static long removeMatching(PlayerInventory inventory, Material material, long needed) {
        long left = needed;
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }
            int take = (int) Math.min(left, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
            left -= take;
        }
        inventory.setStorageContents(contents);
        return needed - left;
    }
}
