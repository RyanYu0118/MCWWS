package work.mcwws.ultimateshopfix;

import cn.superiormc.ultimateshop.gui.InvGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 商店 GUI 里按丢弃键会额外漏出一次手持物品的 {@link PlayerInteractEvent} 左键。
 * 服务器指南针左键绑定了 {@code slimefun open_guide}，于是看起来像「商店里按 Q 打开了史莱姆指南」。
 * ItemCommand 的监听是 {@code LOW} 且不看 cancelled，所以除了取消事件还要把交互物品清空。
 */
final class HeldItemGuiGuard implements Listener {

    private static final long GUARD_TICKS = 5L;
    private static final Object ITEM_FIELD_UNRESOLVED = new Object();
    private static volatile Object interactItemField = ITEM_FIELD_UNRESOLVED;

    private final Map<UUID, Long> dropGuardUntilTick = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onShopDropClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ClickType click = event.getClick();
        if (click != ClickType.DROP && click != ClickType.CONTROL_DROP) {
            return;
        }
        if (!isUltimateShopGui(player)) {
            return;
        }
        markDropGuard(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDropWhileShopOpen(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isUltimateShopGui(player) && !hasDropGuard(player)) {
            return;
        }
        event.setCancelled(true);
        markDropGuard(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onHeldItemInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!isExternalGuiOpen(player) && !hasDropGuard(player)) {
            return;
        }
        suppressInteract(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSlimefunGuideCommand(PlayerCommandPreprocessEvent event) {
        if (!isSlimefunGuideCommand(event.getMessage())) {
            return;
        }
        Player player = event.getPlayer();
        if (!isExternalGuiOpen(player) && !hasDropGuard(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dropGuardUntilTick.remove(event.getPlayer().getUniqueId());
    }

    private void markDropGuard(Player player) {
        dropGuardUntilTick.put(player.getUniqueId(), Bukkit.getCurrentTick() + GUARD_TICKS);
    }

    private boolean hasDropGuard(Player player) {
        Long until = dropGuardUntilTick.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (Bukkit.getCurrentTick() > until) {
            dropGuardUntilTick.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    static boolean isUltimateShopGui(Player player) {
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        return holder instanceof InvGUI;
    }

    static boolean isExternalGuiOpen(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        InventoryType type = top.getType();
        return type != InventoryType.CRAFTING && type != InventoryType.CREATIVE;
    }

    static boolean isSlimefunGuideCommand(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String command = raw.startsWith("/") ? raw.substring(1) : raw;
        command = command.toLowerCase(Locale.ROOT).trim();
        return command.equals("slimefun open_guide")
                || command.startsWith("slimefun open_guide ")
                || command.equals("sf open_guide")
                || command.startsWith("sf open_guide ");
    }

    private static void suppressInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        Field field = resolveInteractItemField();
        if (field == null) {
            return;
        }
        try {
            field.set(event, null);
        } catch (IllegalAccessException ignored) {
            // ItemCommand 若仍读到物品，命令拦截会兜底。
        }
    }

    private static Field resolveInteractItemField() {
        Object cached = interactItemField;
        if (cached != ITEM_FIELD_UNRESOLVED) {
            return (Field) cached;
        }
        Field found = null;
        for (String name : new String[] {"item", "itemStack", "stack"}) {
            try {
                found = PlayerInteractEvent.class.getDeclaredField(name);
                found.setAccessible(true);
                break;
            } catch (NoSuchFieldException ignored) {
                // try next
            }
        }
        interactItemField = found;
        return found;
    }
}
