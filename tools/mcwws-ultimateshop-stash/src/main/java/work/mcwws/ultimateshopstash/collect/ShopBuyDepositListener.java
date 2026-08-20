package work.mcwws.ultimateshopstash.collect;

import cn.superiormc.ultimateshop.api.ItemFinishTransactionEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
        String key = ItemKeys.fromObjectItem(event.getItem());
        if (key == null || !plugin.catalog().contains(key)) {
            return;
        }
        if (!plugin.shouldAutoCollect(player, key)) {
            return;
        }
        int units = Math.max(1, event.getAmount());
        int unitSize = ItemKeys.unitSize(event.getItem(), player);
        long totalItems = (long) units * unitSize;
        Material material = Material.matchMaterial(key.toUpperCase(java.util.Locale.ROOT));
        if (material == null) {
            return;
        }
        // Only deposit overflow plain vanilla items; Slimefun / custom heads stay in inventory.
        long capacity = ItemKeys.plainCapacity(player.getInventory(), material);
        long overflow = totalItems - capacity;
        if (overflow <= 0) {
            return;
        }
        long removed = ItemKeys.removePlainMaterial(player.getInventory(), material, overflow);
        if (removed <= 0) {
            return;
        }
        plugin.storage().add(player.getUniqueId(), key, removed);
        StashNotifier.collect(plugin, player, key, removed);
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.lorePatcher().patchOpenShop(player));
    }
}
