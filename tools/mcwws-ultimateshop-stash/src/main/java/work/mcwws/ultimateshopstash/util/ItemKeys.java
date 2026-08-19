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
import work.mcwws.ultimateshopstash.catalog.ShopCatalog;

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
        ItemStack stack = previewStack(objectItem, player);
        return Math.max(1, stack.getAmount());
    }

    public static String matchStack(ShopCatalog catalog, ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }
        String enumName = stack.getType().name();
        if (catalog.contains(enumName)) {
            return Messages.normalizeKey(enumName);
        }
        return null;
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
