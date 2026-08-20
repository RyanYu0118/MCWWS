package work.mcwws.ideachievements;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PlayerProgressStore {

    private final McwwsIdeaAchievementsPlugin plugin;
    private final File file;
    private FileConfiguration yaml;

    public PlayerProgressStore(McwwsIdeaAchievementsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "progress.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("无法创建数据目录: " + plugin.getDataFolder());
        }
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("无法创建 progress.yml");
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "创建 progress.yml 失败", e);
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "保存 progress.yml 失败", e);
        }
    }

    public PlayerDayState get(UUID uuid) {
        ConfigurationSection sec = yaml.getConfigurationSection(uuid.toString());
        if (sec == null) {
            return new PlayerDayState(null, 0, 0, false);
        }
        String date = sec.getString("date");
        LocalDate tracking = null;
        if (date != null && !date.isBlank()) {
            tracking = LocalDate.parse(date);
        }
        return new PlayerDayState(
                tracking,
                sec.getDouble("earned", 0),
                sec.getInt("streak", 0),
                sec.getBoolean("dark-chicken-done", false));
    }

    public void put(UUID uuid, PlayerDayState state) {
        String path = uuid.toString();
        if (state.trackingDate() == null) {
            yaml.set(path + ".date", null);
        } else {
            yaml.set(path + ".date", state.trackingDate().toString());
        }
        yaml.set(path + ".earned", state.dayEarned());
        yaml.set(path + ".streak", state.streak());
        yaml.set(path + ".dark-chicken-done", state.darkChickenDone());
    }

    public record PlayerDayState(LocalDate trackingDate, double dayEarned, int streak, boolean darkChickenDone) {
        public PlayerDayState withDate(LocalDate date) {
            return new PlayerDayState(date, dayEarned, streak, darkChickenDone);
        }

        public PlayerDayState withEarned(double earned) {
            return new PlayerDayState(trackingDate, earned, streak, darkChickenDone);
        }

        public PlayerDayState withStreak(int streak) {
            return new PlayerDayState(trackingDate, dayEarned, streak, darkChickenDone);
        }

        public PlayerDayState withDarkChickenDone(boolean done) {
            return new PlayerDayState(trackingDate, dayEarned, streak, done);
        }
    }
}
