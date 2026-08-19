package work.mcwws.ultimateshopstash.gui;

import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.Chat;
import work.mcwws.ultimateshopstash.util.ItemKeys;
import work.mcwws.ultimateshopstash.util.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WithdrawMenu {

    public static final int SLOT_BACK = 4;
    public static final int SLOT_CLOSE = 8;
    public static final int[] AMOUNT_SLOTS = {9, 10, 11, 12, 13, 14, 15};
    public static final int[] AMOUNT_VALUES = {1, 2, 4, 8, 16, 32, 64};
    public static final int SLOT_PREVIEW = 22;
    public static final int SLOT_CONFIRM = 31;

    private final McwwsUltimateShopStashPlugin plugin;
    private final Map<UUID, WithdrawSession> sessions = new ConcurrentHashMap<>();

    public WithdrawMenu(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    public WithdrawSession session(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void open(Player player, ObjectItem objectItem) {
        String itemKey = ItemKeys.fromObjectItem(objectItem);
        if (itemKey == null) {
            Chat.send(player, plugin.messages(), "not-shop-item", null);
            return;
        }
        WithdrawSession session = new WithdrawSession(
                objectItem.getShop(),
                objectItem.getProduct(),
                itemKey,
                objectItem
        );
        sessions.put(player.getUniqueId(), session);
        WithdrawHolder holder = new WithdrawHolder(session);
        Inventory inventory = Bukkit.createInventory(holder, 36, color("&8从仓库取出"));
        holder.bind(inventory);
        paint(player, inventory, session);
        player.openInventory(inventory);
    }

    public void paint(Player player, Inventory inventory, WithdrawSession session) {
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, 2200002, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler.clone());
        }
        inventory.setItem(SLOT_BACK, pane(Material.ENCHANTED_BOOK, 2200003, "&f返回"));
        inventory.setItem(SLOT_CLOSE, pane(Material.BARRIER, 0, "&c关闭"));
        for (int i = 0; i < AMOUNT_SLOTS.length; i++) {
            inventory.setItem(AMOUNT_SLOTS[i], amountWool(AMOUNT_VALUES[i]));
        }
        ItemStack preview = ItemKeys.previewStack(session.objectItem(), player);
        long stash = plugin.storage().getAmount(player.getUniqueId(), session.itemKey());
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(plugin.messages().legacy("stash-lore", Map.of("amount", String.valueOf(stash))));
            meta.setLore(lore);
            preview.setItemMeta(meta);
        }
        inventory.setItem(SLOT_PREVIEW, preview);
        refreshConfirm(player, inventory, session);
    }

    public void refreshConfirm(Player player, Inventory inventory, WithdrawSession session) {
        long stash = plugin.storage().getAmount(player.getUniqueId(), session.itemKey());
        int amount = Math.min(session.selectedAmount(), plugin.maxWithdrawAmount());
        amount = (int) Math.min(amount, Math.max(0, stash));
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&a取出 ×" + amount));
            meta.setLore(List.of(
                    color("&7从仓库免费取回物品"),
                    color("&7仓库余量: &f" + stash)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            paper.setItemMeta(meta);
        }
        inventory.setItem(SLOT_CONFIRM, paper);
    }

    public void clear(Player player) {
        sessions.remove(player.getUniqueId());
    }

    private ItemStack amountWool(int value) {
        ItemStack wool = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = wool.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&a&l×" + value));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            wool.setItemMeta(meta);
        }
        return wool;
    }

    private ItemStack pane(Material material, int cmd, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (cmd > 0) {
                meta.setCustomModelData(cmd);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
