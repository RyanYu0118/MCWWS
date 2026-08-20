package work.mcwws.ultimateshopstash.util;

import cn.superiormc.ultimateshop.managers.ConfigManager;
import cn.superiormc.ultimateshop.objects.ObjectShop;
import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import cn.superiormc.ultimateshop.objects.items.GiveItemStack;
import cn.superiormc.ultimateshop.objects.items.products.ObjectSingleProduct;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import work.mcwws.ultimateshopstash.catalog.ShopCatalog;

import java.util.HashMap;

public final class ItemKeys {

    private ItemKeys() {
    }

    public static String fromObjectItem(ObjectItem objectItem) {
        if (objectItem == null || objectItem.empty) {
            return null;
        }
        ConfigurationSection section = objectItem.getItemConfig();
        if (section == null) {
            return null;
        }
        ConfigurationSection products = section.getConfigurationSection("products");
        if (products == null) {
            return null;
        }
        for (String key : products.getKeys(false)) {
            ConfigurationSection product = products.getConfigurationSection(key);
            if (product == null) {
                continue;
            }
            String material = product.getString("material");
            if (material != null && !material.isBlank()) {
                return Messages.normalizeKey(material);
            }
        }
        return null;
    }

    public static ItemStack previewStack(ObjectItem objectItem, Player player) {
        if (objectItem == null || objectItem.empty) {
            return new ItemStack(Material.BARRIER);
        }
        try {
            ItemStack display = objectItem.getDisplayItem(player);
            if (display != null && display.getType() != Material.AIR) {
                return display.clone();
            }
        } catch (Throwable ignored) {
            // fallback below
        }
        String key = fromObjectItem(objectItem);
        if (key == null) {
            return new ItemStack(Material.BARRIER);
        }
        Material material = Material.matchMaterial(key.toUpperCase(java.util.Locale.ROOT));
        if (material == null) {
            material = Material.BARRIER;
        }
        return new ItemStack(material);
    }

    public static int unitSize(ObjectItem objectItem, Player player) {
        if (objectItem == null || objectItem.empty) {
            return 1;
        }
        try {
            if (objectItem.getReward() != null) {
                ObjectSingleProduct product = objectItem.getReward().getTargetProduct(player);
                if (product != null) {
                    GiveItemStack give = product.getItemThing(product.getSingleSection(), player, 1, false);
                    if (give != null && give.getTargetItem() != null) {
                        return Math.max(1, give.getTargetItem().getAmount());
                    }
                }
            }
        } catch (Throwable ignored) {
            // fallback
        }
        ItemStack stack = unitStack(objectItem, player);
        return Math.max(1, stack.getAmount());
    }

    /** 单个单位的完整物品（含 UltimateShop 配置的头颅纹理、Slimefun 引用等）。 */
    public static ItemStack unitStack(ObjectItem objectItem, Player player) {
        if (objectItem == null || objectItem.empty) {
            return new ItemStack(Material.BARRIER);
        }
        try {
            if (objectItem.getReward() != null) {
                ObjectSingleProduct product = objectItem.getReward().getTargetProduct(player);
                if (product != null) {
                    GiveItemStack give = product.getItemThing(product.getSingleSection(), player, 1, false);
                    if (give != null && give.getTargetItem() != null) {
                        return give.getTargetItem().clone();
                    }
                }
            }
        } catch (Throwable ignored) {
            // fallback below
        }
        return previewStack(objectItem, player);
    }

    public static void giveToPlayer(Player player, ObjectItem objectItem, long totalItems) {
        giveToPlayer(player, unitStack(objectItem, player), totalItems);
    }

    public static void giveToPlayer(Player player, ItemStack prototype, long totalItems) {
        if (prototype == null || totalItems <= 0) {
            return;
        }
        long left = totalItems;
        int maxStack = prototype.getMaxStackSize();
        while (left > 0) {
            ItemStack stack = prototype.clone();
            int give = (int) Math.min(left, maxStack);
            stack.setAmount(give);
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            left -= give;
        }
    }

    public static long countPlainMaterial(PlayerInventory inventory, Material material) {
        long count = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack != null && stack.getType() == material && isVanillaPlain(stack)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    public static long plainCapacity(PlayerInventory inventory, Material material) {
        long cap = 0;
        int maxStack = material.getMaxStackSize();
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                cap += maxStack;
            } else if (stack.getType() == material && isVanillaPlain(stack)) {
                cap += Math.max(0, maxStack - stack.getAmount());
            }
        }
        return cap;
    }

    public static long removePlainMaterial(PlayerInventory inventory, Material material, long needed) {
        long left = needed;
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material || !isVanillaPlain(stack)) {
                continue;
            }
            int take = (int) Math.min(left, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
            left -= take;
        }
        inventory.setStorageContents(contents);
        return needed - left;
    }

    public static String matchStack(ShopCatalog catalog, ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }
        if (!isVanillaPlain(stack)) {
            return null;
        }
        String enumName = stack.getType().name();
        if (catalog.contains(enumName)) {
            return Messages.normalizeKey(enumName);
        }
        return null;
    }

    /**
     * Returns true only if the item is a plain vanilla item with no modifications:
     * no durability loss, no enchantments, no custom display name, no custom NBT/PDC
     * (which would indicate Slimefun or other plugin items).
     */
    public static boolean isVanillaPlain(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return true;
        }
        org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return true;
        }
        // Has enchantments
        if (!meta.getEnchants().isEmpty()) {
            return false;
        }
        // Has custom display name (renamed or plugin-named)
        if (meta.hasDisplayName()) {
            return false;
        }
        // Has lore (Slimefun and many plugins add lore)
        if (meta.hasLore()) {
            return false;
        }
        // Has durability damage
        if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
            if (damageable.hasDamage()) {
                return false;
            }
        }
        // Has persistent data container entries (Slimefun uses PDC)
        if (!meta.getPersistentDataContainer().isEmpty()) {
            return false;
        }
        return true;
    }

    public static ObjectShop findShop(String shopName) {
        if (shopName == null || shopName.isBlank()) {
            return null;
        }
        ConfigManager manager = ConfigManager.configManager;
        if (manager == null) {
            return null;
        }
        return manager.getShop(shopName);
    }
}
