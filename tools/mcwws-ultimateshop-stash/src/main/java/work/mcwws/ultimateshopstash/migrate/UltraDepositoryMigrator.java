package work.mcwws.ultimateshopstash.migrate;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class UltraDepositoryMigrator {

    private final McwwsUltimateShopStashPlugin plugin;

    public UltraDepositoryMigrator(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    public void runIfNeeded() {
        File marker = markerFile();
        if (marker.exists()) {
            return;
        }
        int players = migrate();
        try {
            marker.getParentFile().mkdirs();
            marker.createNewFile();
        } catch (IOException ex) {
            plugin.getLogger().warning("无法写入迁移标记: " + ex.getMessage());
        }
        plugin.getLogger().info("UltraDepository 迁移完成，合并 " + players + " 名玩家数据。");
    }

    public int migrate() {
        File sourceDir = new File(plugin.getDataFolder().getParentFile(), "UltraDepository/data");
        if (!sourceDir.isDirectory()) {
            plugin.getLogger().warning("找不到 UltraDepository 数据目录: " + sourceDir.getPath());
            return 0;
        }
        int players = 0;
        File[] files = sourceDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return 0;
        }
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4);
            UUID uuid;
            try {
                uuid = UUID.fromString(id);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection depositories = yaml.getConfigurationSection("depositories");
            if (depositories == null) {
                continue;
            }
            boolean touched = false;
            for (String dep : depositories.getKeys(false)) {
                ConfigurationSection items = depositories.getConfigurationSection(dep);
                if (items == null) {
                    continue;
                }
                for (String material : items.getKeys(false)) {
                    long amount = items.getLong(material + ".amount", 0L);
                    if (amount > 0) {
                        plugin.storage().mergeFrom(uuid, material, amount);
                        touched = true;
                    }
                }
            }
            if (touched) {
                players++;
            }
        }
        plugin.storage().saveAll();
        return players;
    }

    public boolean alreadyMigrated() {
        return markerFile().exists();
    }

    private File markerFile() {
        return new File(plugin.getDataFolder(), plugin.getConfig().getString("migration.marker-file",
                "migrated_from_ultradepository.flag"));
    }
}
