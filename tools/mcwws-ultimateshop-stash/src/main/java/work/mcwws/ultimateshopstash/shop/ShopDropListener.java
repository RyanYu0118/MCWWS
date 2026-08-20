package work.mcwws.ultimateshopstash.shop;

import cn.superiormc.ultimateshop.gui.InvGUI;
import cn.superiormc.ultimateshop.objects.buttons.AbstractButton;
import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.InventoryHolder;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.Chat;
import work.mcwws.ultimateshopstash.util.ItemKeys;
import work.mcwws.ultimateshopstash.util.Messages;

import java.util.Map;

public final class ShopDropListener implements Listener {

    private final McwwsUltimateShopStashPlugin plugin;

    public ShopDropListener(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onShopDrop(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ClickType click = event.getClick();
        if (click != ClickType.DROP && click != ClickType.CONTROL_DROP) {
            return;
        }
        if (event.getRawSlot() != event.getSlot()) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof InvGUI invGui)) {
            return;
        }
        AbstractButton button = invGui.menuButtons.get(event.getSlot());
        if (!(button instanceof ObjectItem objectItem) || objectItem.empty) {
            return;
        }
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        if (click == ClickType.CONTROL_DROP) {
            plugin.getServer().getScheduler().runTask(plugin, () -> toggleCollect(player, objectItem));
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.withdrawMenu().open(player, objectItem));
    }

    private void toggleCollect(Player player, ObjectItem objectItem) {
        String key = ItemKeys.fromObjectItem(objectItem);
        if (key == null || !plugin.catalog().contains(key)) {
            Chat.send(player, plugin.messages(), "not-shop-item", null);
            return;
        }
        boolean skipped = plugin.storage().toggleSkipCollect(player.getUniqueId(), key);
        Chat.send(player, plugin.messages(), skipped ? "collect-toggle-off" : "collect-toggle-on", Map.of(
                "item", Messages.displayMaterial(key)
        ));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, skipped ? 0.8f : 1.2f);
        plugin.lorePatcher().patchOpenShop(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPhysicalDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        if (holder instanceof InvGUI) {
            event.setCancelled(true);
        }
    }
}
