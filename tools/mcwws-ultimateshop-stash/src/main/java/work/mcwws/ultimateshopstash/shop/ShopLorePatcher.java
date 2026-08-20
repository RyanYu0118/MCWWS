package work.mcwws.ultimateshopstash.shop;

import cn.superiormc.ultimateshop.gui.InvGUI;
import cn.superiormc.ultimateshop.objects.buttons.AbstractButton;
import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.ItemKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ShopLorePatcher implements Listener {

    private final McwwsUltimateShopStashPlugin plugin;

    public ShopLorePatcher(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof InvGUI)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> patchOpenShop(player));
    }

    public void patchOpenShop(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof InvGUI invGui)) {
            return;
        }
        for (Map.Entry<Integer, AbstractButton> entry : invGui.menuButtons.entrySet()) {
            if (!(entry.getValue() instanceof ObjectItem objectItem) || objectItem.empty) {
                continue;
            }
            int slot = entry.getKey();
            ItemStack current = top.getItem(slot);
            if (current == null || current.getType().isAir()) {
                continue;
            }
            String key = ItemKeys.fromObjectItem(objectItem);
            if (key == null) {
                continue;
            }
            long amount = plugin.storage().getAmount(player.getUniqueId(), key);
            ItemStack patched = current.clone();
            ItemMeta meta = patched.getItemMeta();
            if (meta == null) {
                continue;
            }
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            // Paper 重读 lore 时颜色码可能变化，旧版用 §8§l 标记匹配会失败，导致「仓库余量/自动吸取」反复叠加。
            lore.removeIf(ShopLorePatcher::isStashLoreLine);
            lore.add(plugin.messages().legacy("stash-lore", Map.of("amount", String.valueOf(amount))));
            boolean skipped = plugin.storage().isSkipCollect(player.getUniqueId(), key);
            lore.add(plugin.messages().legacy(skipped ? "stash-collect-lore-off" : "stash-collect-lore-on", null));
            meta.setLore(lore);
            patched.setItemMeta(meta);
            top.setItem(slot, patched);
        }
    }

    private static boolean isStashLoreLine(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        String plain = ChatColor.stripColor(line);
        if (plain == null || plain.isEmpty()) {
            return false;
        }
        return plain.contains("仓库余量") || plain.contains("自动吸取") || plain.contains("仓库吸取");
    }
}
