package work.mcwws.ultimateshopstash.trade;

import cn.superiormc.ultimateshop.api.ItemPreTransactionEvent;
import cn.superiormc.ultimateshop.api.ItemFinishTransactionEvent;
import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.ItemKeys;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intercepts UltimateShop transactions:
 * - BUY: if stash + BetterBags have enough, cancel the paid transaction and give items for free.
 * - SELL: before transaction, temporarily inject items from stash/BetterBags into inventory
 *   so UltimateShop can take them. After transaction, reconcile the stash/BetterBags amounts.
 */
public final class TradeInterceptor implements Listener {

    private final McwwsUltimateShopStashPlugin plugin;
    private final Map<UUID, SellSnapshot> sellSnapshots = new ConcurrentHashMap<>();

    public TradeInterceptor(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── BUY: free retrieval from stash/BetterBags ─────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreBuy(ItemPreTransactionEvent event) {
        if (!event.isBuyOrSell()) return; // false = sell
        Player player = event.getPlayer();
        if (!plugin.hasAutoCollect(player)) return;

        String key = ItemKeys.fromObjectItem(event.getItem());
        if (key == null || !plugin.catalog().contains(key)) return;

        Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
        if (material == null) return;

        int units = Math.max(1, event.getAmount());
        int unitSize = ItemKeys.unitSize(event.getItem(), player);
        long needed = (long) units * unitSize;

        long stashHave = plugin.storage().getAmount(player.getUniqueId(), key);

        if (stashHave <= 0) return;

        if (stashHave >= needed) {
            event.setCancelled(true);
            plugin.storage().remove(player.getUniqueId(), key, needed);
            ItemKeys.giveToPlayer(player, event.getItem(), needed);
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.lorePatcher().patchOpenShop(player));
        }
    }

    // ─── SELL: inject stash/BetterBags items into inventory before transaction ──

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPreSell(ItemPreTransactionEvent event) {
        if (event.isBuyOrSell()) return; // true = buy
        Player player = event.getPlayer();
        if (!plugin.hasAutoCollect(player)) return;

        String key = ItemKeys.fromObjectItem(event.getItem());
        if (key == null || !plugin.catalog().contains(key)) return;

        Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
        if (material == null) return;

        int units = Math.max(1, event.getAmount());
        int unitSize = ItemKeys.unitSize(event.getItem(), player);
        long needed = (long) units * unitSize;

        // Count what player already has in hotbar + inventory
        PlayerInventory inv = player.getInventory();
        long inInventory = ItemKeys.countPlainMaterial(inv, material);

        if (inInventory >= needed) return; // already enough, no injection needed

        long shortfall = needed - inInventory;

        // Only supplement from stash (BetterBags getInventoryPage is too slow)
        long stashHave = plugin.storage().getAmount(player.getUniqueId(), key);
        long fromStash = Math.min(shortfall, stashHave);

        if (fromStash <= 0) return;

        plugin.storage().remove(player.getUniqueId(), key, fromStash);
        injectItems(inv, event.getItem(), player, fromStash);

        sellSnapshots.put(player.getUniqueId(), new SellSnapshot(key, event.getItem(), fromStash));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFinishSell(ItemFinishTransactionEvent event) {
        if (event.isBuyOrSell()) return; // we only care about sell completions
        SellSnapshot snapshot = sellSnapshots.remove(event.getPlayer().getUniqueId());
        if (snapshot == null) return;
        // Transaction succeeded; items were consumed from inventory. Nothing to reconcile.
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.lorePatcher().patchOpenShop(event.getPlayer()));
    }

    // If the pre-transaction was cancelled after our injection (another plugin cancelled it),
    // we need to pull the injected items back. We track this via a scheduled check.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreSellCancelled(ItemPreTransactionEvent event) {
        if (event.isBuyOrSell() || !event.isCancelled()) return;
        SellSnapshot snapshot = sellSnapshots.remove(event.getPlayer().getUniqueId());
        if (snapshot == null) return;
        Player player = event.getPlayer();
        long removed = removeMatchingStacks(player.getInventory(), snapshot.objectItem(), player, snapshot.fromStash());
        if (removed > 0) plugin.storage().add(player.getUniqueId(), snapshot.key(), removed);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void injectItems(PlayerInventory inv, ObjectItem objectItem, Player player, long amount) {
        ItemStack prototype = ItemKeys.unitStack(objectItem, player);
        int maxStack = prototype.getMaxStackSize();
        ItemStack[] contents = inv.getStorageContents();
        long left = amount;

        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == prototype.getType()
                    && ItemKeys.isVanillaPlain(stack) && stack.isSimilar(prototype)
                    && stack.getAmount() < maxStack) {
                int add = (int) Math.min(left, maxStack - stack.getAmount());
                stack.setAmount(stack.getAmount() + add);
                left -= add;
            }
        }
        for (int i = 0; i < contents.length && left > 0; i++) {
            if (contents[i] == null || contents[i].getType().isAir()) {
                ItemStack inserted = prototype.clone();
                int add = (int) Math.min(left, maxStack);
                inserted.setAmount(add);
                contents[i] = inserted;
                left -= add;
            }
        }
        inv.setStorageContents(contents);
    }

    private static long removeMatchingStacks(
            PlayerInventory inv, ObjectItem objectItem, Player player, long amount) {
        ItemStack prototype = ItemKeys.unitStack(objectItem, player);
        ItemStack[] contents = inv.getStorageContents();
        long left = amount;
        for (int i = contents.length - 1; i >= 0 && left > 0; i--) {
            ItemStack stack = contents[i];
            if (stack == null) {
                continue;
            }
            boolean matches = stack.isSimilar(prototype)
                    || (ItemKeys.isVanillaPlain(stack) && stack.getType() == prototype.getType());
            if (!matches) {
                continue;
            }
            int take = (int) Math.min(left, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
            left -= take;
        }
        inv.setStorageContents(contents);
        return amount - left;
    }

    private record SellSnapshot(String key, ObjectItem objectItem, long fromStash) {
    }
}
