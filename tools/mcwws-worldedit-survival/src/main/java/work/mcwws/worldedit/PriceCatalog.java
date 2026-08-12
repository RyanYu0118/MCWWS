package work.mcwws.worldedit;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PriceCatalog {

    private final McwwsWeSurvivalPlugin plugin;
    private final Map<String, Double> buyPrices = new HashMap<>();
    private final Map<String, Double> sellPrices = new HashMap<>();
    private long lastLoadedMs;

    public PriceCatalog(McwwsWeSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        buyPrices.clear();
        sellPrices.clear();
        String path = plugin.getPluginConfig().getString("prices-file", "plugins/Skript/scripts/web/mcwws/economy/web_prices.yml");
        File file = plugin.resolveDataFile(path);
        YamlConfiguration yaml = plugin.loadExternalYaml(file);
        for (String key : yaml.getKeys(false)) {
            if (!yaml.isConfigurationSection(key)) {
                continue;
            }
            String id = normalize(key);
            double buy = yaml.getDouble(key + ".buy", 0D);
            if (buy > 0D) {
                buyPrices.put(id, buy);
            }
            double sell = yaml.getDouble(key + ".sell", 0D);
            if (sell > 0D) {
                sellPrices.put(id, sell);
            }
        }
        lastLoadedMs = System.currentTimeMillis();
    }

    public void reloadIfStale() {
        int seconds = plugin.getPluginConfig().getInt("prices-reload-seconds", 60);
        if (seconds <= 0) {
            return;
        }
        if (System.currentTimeMillis() - lastLoadedMs > seconds * 1000L) {
            reload();
        }
    }

    public double getBuyPrice(String itemId) {
        reloadIfStale();
        if (itemId == null || itemId.isBlank()) {
            return 0D;
        }
        return buyPrices.getOrDefault(normalize(itemId), 0D);
    }

    /** 市场卖价；物价表只给了买价的物品退回买价，避免拆除白干 */
    public double getSellPrice(String itemId) {
        reloadIfStale();
        if (itemId == null || itemId.isBlank()) {
            return 0D;
        }
        String id = normalize(itemId);
        Double sell = sellPrices.get(id);
        if (sell != null) {
            return sell;
        }
        return buyPrices.getOrDefault(id, 0D);
    }

    public static String normalize(String itemId) {
        String id = itemId.toLowerCase(Locale.ROOT).trim();
        if (id.startsWith("minecraft:")) {
            id = id.substring("minecraft:".length());
        }
        return id;
    }
}
