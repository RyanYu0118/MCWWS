package work.mcwws.ultimateshopfix;

import cn.superiormc.ultimateshop.gui.inv.BuyMoreGUI;
import cn.superiormc.ultimateshop.objects.buttons.AbstractButton;
import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

/**
 * UltimateShop 批量买卖菜单对所有物品都放 1～64。按原版堆叠上限只保留 1、2、4…2^n。
 */
final class BuyMoreStackLimit implements Listener {

    private static final int CAP = 64;
    private static final Field ITEM_FIELD;
    private static final Field AMOUNT_FIELD;

    static {
        try {
            ITEM_FIELD = BuyMoreGUI.class.getDeclaredField("item");
            ITEM_FIELD.setAccessible(true);
            AMOUNT_FIELD = BuyMoreGUI.class.getDeclaredField("nowingAmount");
            AMOUNT_FIELD.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final JavaPlugin plugin;

    BuyMoreStackLimit(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        BuyMoreGUI gui = asBuyMore(event.getInventory());
        if (gui == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> apply(player, gui, event.getInventory()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        BuyMoreGUI gui = asBuyMore(event.getInventory());
        if (gui == null) {
            return;
        }
        int cap = stackCap(gui, player);
        int setAmount = setAmountAt(gui, event.getRawSlot());
        if (setAmount > cap) {
            event.setCancelled(true);
        }
    }

    private void apply(Player player, BuyMoreGUI gui, Inventory inventory) {
        if (!player.isOnline()) {
            return;
        }
        if (asBuyMore(player.getOpenInventory().getTopInventory()) != gui) {
            return;
        }
        int cap = stackCap(gui, player);
        Map<Integer, AbstractButton> buttons = gui.menuButtons;
        if (buttons == null) {
            return;
        }
        ItemStack filler = filler();
        for (Integer slot : new ArrayList<>(buttons.keySet())) {
            int amount = setAmountOf(buttons.get(slot));
            if (amount <= cap) {
                continue;
            }
            buttons.remove(slot);
            inventory.setItem(slot, filler);
        }
        try {
            int current = AMOUNT_FIELD.getInt(gui);
            if (current > cap) {
                AMOUNT_FIELD.setInt(gui, cap);
            }
        } catch (IllegalAccessException ignored) {
            // 数量按钮已按上限裁掉，确认时仍会走插件自己的 max-amount。
        }
    }

    private static BuyMoreGUI asBuyMore(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof BuyMoreGUI gui ? gui : null;
    }

    private static int stackCap(BuyMoreGUI gui, Player player) {
        try {
            ObjectItem item = (ObjectItem) ITEM_FIELD.get(gui);
            if (item == null) {
                return 1;
            }
            ItemStack stack = item.getDisplayItem(player);
            if (stack == null || stack.getType().isAir()) {
                return 1;
            }
            return Math.min(CAP, Math.max(1, stack.getMaxStackSize()));
        } catch (IllegalAccessException e) {
            return CAP;
        }
    }

    private static int setAmountAt(BuyMoreGUI gui, int slot) {
        Map<Integer, AbstractButton> buttons = gui.menuButtons;
        if (buttons == null) {
            return -1;
        }
        return setAmountOf(buttons.get(slot));
    }

    private static int setAmountOf(AbstractButton button) {
        if (button == null || button.config == null || !button.config.contains("set-amount")) {
            return -1;
        }
        return button.config.getInt("set-amount");
    }

    private static ItemStack filler() {
        ItemStack pane = ItemStacks.asCraft(new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }
}
