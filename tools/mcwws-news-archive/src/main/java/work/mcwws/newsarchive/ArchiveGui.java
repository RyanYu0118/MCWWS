package work.mcwws.newsarchive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ArchiveGui implements InventoryHolder {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final McwwsNewsArchivePlugin plugin;
    private final int page;
    private final Inventory inventory;
    private final Map<Integer, String> slotToVersionId = new HashMap<>();

    private ArchiveGui(McwwsNewsArchivePlugin plugin, int page, int totalPages) {
        this.plugin = plugin;
        this.page = Math.max(0, page);
        String title = plugin.getConfig().getString("gui.title", "&8服务器告示 · 历史 ({page}/{pages})")
                .replace("{page}", String.valueOf(this.page + 1))
                .replace("{pages}", String.valueOf(Math.max(1, totalPages)));
        int size = plugin.getConfig().getInt("gui.size", 54);
        this.inventory = Bukkit.createInventory(this, size, LEGACY.deserialize(title));
    }

    public static void open(McwwsNewsArchivePlugin plugin, Player player, int page) {
        List<NewsVersion> all = plugin.store().listNewestFirst();
        int perPage = Math.max(1, plugin.getConfig().getInt("gui.items-per-page", 45));
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) perPage));
        int safePage = Math.min(Math.max(0, page), totalPages - 1);
        ArchiveGui gui = new ArchiveGui(plugin, safePage, totalPages);
        gui.render(player, all, perPage, totalPages);
        player.openInventory(gui.inventory);
    }

    private void render(Player player, List<NewsVersion> all, int perPage, int totalPages) {
        fillChrome(totalPages);
        int start = page * perPage;
        int end = Math.min(all.size(), start + perPage);
        int slot = 9;
        for (int i = start; i < end && slot < inventory.getSize(); i++) {
            NewsVersion version = all.get(i);
            boolean read = plugin.store().hasRead(player.getUniqueId(), version.id());
            inventory.setItem(slot, versionIcon(version, read));
            slotToVersionId.put(slot, version.id());
            slot++;
        }
    }

    private void fillChrome(int totalPages) {
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, " ", plugin.getConfig().getInt("gui.filler-model", 2200002));
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
        }
        boolean hasPrev = page > 0;
        boolean hasNext = page + 1 < totalPages;
        inventory.setItem(3, pane(
                hasPrev ? Material.LIME_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE,
                hasPrev ? "&e< 上一页" : "&7< 上一页",
                hasPrev
                        ? plugin.getConfig().getInt("gui.prev-enabled-model", 2200007)
                        : plugin.getConfig().getInt("gui.prev-disabled-model", 2200008)));
        inventory.setItem(4, modelItem(
                Material.ENCHANTED_BOOK,
                "&c返回",
                List.of("&7返回指南针菜单"),
                plugin.getConfig().getInt("gui.back-model", 2200003),
                false));
        inventory.setItem(5, pane(
                hasNext ? Material.LIME_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE,
                hasNext ? "&e下一页 >" : "&7下一页 >",
                hasNext
                        ? plugin.getConfig().getInt("gui.next-enabled-model", 2200009)
                        : plugin.getConfig().getInt("gui.next-disabled-model", 2200010)));
        inventory.setItem(8, named(Material.BARRIER, "&c关闭", List.of("&7关闭界面")));
    }

    private ItemStack versionIcon(NewsVersion version, boolean read) {
        Material material = read ? Material.BOOK : Material.ENCHANTED_BOOK;
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (!version.published().isBlank()) {
            lore.add("&7发布：&f" + version.published());
        }
        lore.add("&7版本：&f" + version.id());
        lore.add(read ? "&8已读" : "&a未读");
        if (!version.summary().isBlank()) {
            lore.add("");
            for (String part : wrap(version.summary(), 28)) {
                lore.add("&7" + part);
            }
        }
        lore.add("");
        lore.add("&e左键 &7打开该期告示");
        return modelItem(material, "&f" + version.title(), lore, 0, !read);
    }

    private static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        String remaining = text;
        while (!remaining.isEmpty()) {
            if (remaining.length() <= width) {
                out.add(remaining);
                break;
            }
            out.add(remaining.substring(0, width));
            remaining = remaining.substring(width);
        }
        return out;
    }

    private ItemStack pane(Material material, String name, int model) {
        return modelItem(material, name, List.of(), model, false);
    }

    private ItemStack named(Material material, String name, List<String> lore) {
        return modelItem(material, name, lore, 0, false);
    }

    private ItemStack modelItem(Material material, String name, List<String> lore, int model, boolean glow) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(LEGACY.deserialize(name));
        if (lore != null && !lore.isEmpty()) {
            List<Component> components = new ArrayList<>();
            for (String line : lore) {
                components.add(LEGACY.deserialize(line));
            }
            meta.lore(components);
        }
        if (model > 0) {
            meta.setCustomModelData(model);
        }
        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    public int page() {
        return page;
    }

    public String versionIdAt(int slot) {
        return slotToVersionId.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
