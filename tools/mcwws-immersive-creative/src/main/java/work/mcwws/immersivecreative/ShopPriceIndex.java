package work.mcwws.immersivecreative;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class ShopPriceIndex {

    private final McwwsImmersiveCreativePlugin plugin;
    private Map<Material, ShopOffer> offersByMaterial = Map.of();
    private Set<Material> blacklist = EnumSet.noneOf(Material.class);

    public ShopPriceIndex(McwwsImmersiveCreativePlugin plugin) {
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
            if (material == null || material.isAir()) {
                continue;
            }
            double unitBuy = prices.getDouble(key + ".buy", -1D);
            if (requireBuyPrice && unitBuy <= 0D) {
                continue;
            }
            double unitSell = prices.getDouble(key + ".sell", -1D);
            next.putIfAbsent(material, new ShopOffer(material, shopId, slot,
                    Math.max(unitBuy, 0D), Math.max(unitSell, 0D)));
        }

        EnumSet<Material> banned = EnumSet.noneOf(Material.class);
        for (String raw : plugin.getConfig().getStringList("blacklist")) {
            Material material = materialFromKey(raw);
            if (material != null) {
                banned.add(material);
            }
        }

        offersByMaterial = Collections.unmodifiableMap(next);
        blacklist = banned.isEmpty() ? EnumSet.noneOf(Material.class) : EnumSet.copyOf(banned);
        plugin.getLogger().info("沉浸式创造已索引 " + offersByMaterial.size() + " 种可购物品，黑名单 "
                + blacklist.size() + " 种。");
    }

    public ShopOffer find(Material material) {
        if (material == null) {
            return null;
        }
        return offersByMaterial.get(material);
    }

    public boolean isBlacklisted(Material material) {
        return material != null && blacklist.contains(material);
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
