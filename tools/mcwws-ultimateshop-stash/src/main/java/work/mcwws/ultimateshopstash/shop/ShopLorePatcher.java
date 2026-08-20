package work.mcwws.ultimateshopstash.shop;

import cn.superiormc.ultimateshop.gui.InvGUI;
import cn.superiormc.ultimateshop.objects.buttons.AbstractButton;
import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import org.bukkit.Bukkit;
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

    private static final String MARKER = "§8§l仓库余量";
    private static final String COLLECT_MARKER = "§8§l仓库吸取";

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
            lore.removeIf(line -> line != null && (line.contains(MARKER) || line.contains(COLLECT_MARKER)));
            lore.add(MARKER);
            lore.add(plugin.messages().legacy("stash-lore", Map.of("amount", String.valueOf(amount))));
            boolean skipped = plugin.storage().isSkipCollect(player.getUniqueId(), key);
            lore.add(COLLECT_MARKER);
            lore.add(plugin.messages().legacy(skipped ? "stash-collect-lore-off" : "stash-collect-lore-on", null));
            meta.setLore(lore);
            patched.setItemMeta(meta);
            top.setItem(slot, patched);
        }
    }
}
