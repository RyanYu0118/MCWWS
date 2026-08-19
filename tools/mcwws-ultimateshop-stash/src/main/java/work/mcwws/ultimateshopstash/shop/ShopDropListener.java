package work.mcwws.ultimateshopstash.shop;

import cn.superiormc.ultimateshop.gui.InvGUI;
import cn.superiormc.ultimateshop.objects.buttons.AbstractButton;
import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
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
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.withdrawMenu().open(player, objectItem));
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
