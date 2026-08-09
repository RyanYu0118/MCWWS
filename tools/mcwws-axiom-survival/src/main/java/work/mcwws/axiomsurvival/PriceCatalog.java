package work.mcwws.axiomsurvival;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PriceCatalog {

    private final McwwsAxiomSurvivalPlugin plugin;
    private final Map<String, Double> buyPrices = new HashMap<>();
    private long lastLoadedMs;

    public PriceCatalog(McwwsAxiomSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        buyPrices.clear();
        String path = plugin.getPluginConfig().getString("prices-file", "plugins/Skript/scripts/web/mcwws/economy/web_prices.yml");
        File file = plugin.resolveDataFile(path);
        YamlConfiguration yaml = plugin.loadExternalYaml(file);
        for (String key : yaml.getKeys(false)) {
            if (!yaml.isConfigurationSection(key)) {
                continue;
            }
            double value = yaml.getDouble(key + ".buy", 0D);
            if (value > 0D) {
                buyPrices.put(normalize(key), value);
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

    public static String normalize(String itemId) {
        String id = itemId.toLowerCase(Locale.ROOT).trim();
        if (id.startsWith("minecraft:")) {
            id = id.substring("minecraft:".length());
        }
        return id;
    }
}
