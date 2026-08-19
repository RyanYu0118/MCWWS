package work.mcwws.ultimateshopstash.catalog;

import cn.superiormc.ultimateshop.managers.ConfigManager;
import cn.superiormc.ultimateshop.objects.ObjectShop;
import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import org.bukkit.configuration.file.YamlConfiguration;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.ItemKeys;
import work.mcwws.ultimateshopstash.util.Messages;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ShopCatalog {

    private final McwwsUltimateShopStashPlugin plugin;
    private final Set<String> materialKeys = new HashSet<>();

    public ShopCatalog(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        materialKeys.clear();
        ConfigManager manager = ConfigManager.configManager;
        if (manager != null) {
            for (ObjectShop shop : manager.getShops()) {
                for (ObjectItem item : shop.getProductList()) {
                    registerItem(item);
                }
            }
        }
        File shopDir = new File(plugin.getDataFolder().getParentFile(), "UltimateShop/shops");
        if (shopDir.isDirectory()) {
            File[] files = shopDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    scanYamlShop(file);
                }
            }
        }
        plugin.getLogger().info("已索引 " + materialKeys.size() + " 种 UltimateShop 仓库物品。");
    }

    private void registerItem(ObjectItem item) {
        String key = ItemKeys.fromObjectItem(item);
        if (key != null && !key.isBlank()) {
            materialKeys.add(key);
        }
    }

    private void scanYamlShop(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var items = yaml.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (String slot : items.getKeys(false)) {
            String material = items.getString(slot + ".products.1.material");
            if (material == null) {
                material = items.getString(slot + ".products.'1'.material");
            }
            if (material != null && !material.isBlank()) {
                materialKeys.add(Messages.normalizeKey(material));
            }
        }
    }

    public boolean contains(String materialEnumOrKey) {
        return materialKeys.contains(Messages.normalizeKey(materialEnumOrKey));
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(materialKeys);
    }
}
