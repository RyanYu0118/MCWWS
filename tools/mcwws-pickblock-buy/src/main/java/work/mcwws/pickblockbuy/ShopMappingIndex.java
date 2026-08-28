package work.mcwws.pickblockbuy;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class ShopMappingIndex {

    private final McwwsPickBlockBuyPlugin plugin;
    private Map<Material, ShopOffer> offersByMaterial = Map.of();

    public ShopMappingIndex(McwwsPickBlockBuyPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File mappingsFile = plugin.resolveServerFile(plugin.getConfig().getString("mappings-file"));
        File pricesFile = plugin.resolveServerFile(plugin.getConfig().getString("prices-file"));
        boolean requireBuyPrice = plugin.getConfig().getBoolean("require-buy-price", true);

        YamlConfiguration mappings = loadYaml(mappingsFile);
        YamlConfiguration prices = loadYaml(pricesFile);
        Map<Material, ShopOffer> next = new HashMap<>();

        for (String key : mappings.getKeys(false)) {
            ConfigurationSection section = mappings.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            String shopId = section.getString("shop");
            String slot = section.getString("item");
            if (shopId == null || shopId.isBlank() || slot == null || slot.isBlank()) {
                continue;
            }
            Material material = materialFromKey(key);
            if (material == null || !material.isBlock()) {
                continue;
            }
            double unitBuy = prices.getDouble(key + ".buy", -1D);
            if (requireBuyPrice && unitBuy <= 0D) {
                continue;
            }
            next.putIfAbsent(material, new ShopOffer(material, shopId, slot, Math.max(unitBuy, 0D)));
        }

        offersByMaterial = Collections.unmodifiableMap(next);
        plugin.getLogger().info("选块购买已索引 " + offersByMaterial.size() + " 种可购方块。");
    }

    public ShopOffer find(Material material) {
        if (material == null) {
            return null;
        }
        return offersByMaterial.get(material);
    }

    private static Material materialFromKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return Material.matchMaterial(key.toUpperCase(Locale.ROOT));
    }

    private YamlConfiguration loadYaml(File file) {
        if (file == null || !file.exists()) {
            plugin.getLogger().warning("找不到 YAML: " + (file == null ? "null" : file.getPath()));
            return new YamlConfiguration();
        }
        try {
            return YamlConfiguration.loadConfiguration(file);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "读取 YAML 失败: " + file.getPath(), ex);
            return new YamlConfiguration();
        }
    }
}
