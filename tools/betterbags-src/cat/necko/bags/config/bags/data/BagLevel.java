/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 */
package cat.necko.bags.config.bags.data;

import cat.necko.bags.Plugin;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.configuration.ConfigurationSection;

public class BagLevel {
    private final ConcurrentMap<Integer, Level> levels = new ConcurrentHashMap<Integer, Level>();
    private final List<String> levelsString;
    private final int maxLevel;

    public BagLevel(ConfigurationSection section) {
        for (int level = 1; level <= section.getKeys(false).size(); ++level) {
            ConfigurationSection levelSection = section.getConfigurationSection(String.valueOf(level));
            if (levelSection == null) {
                Plugin.getInstance().getLogger().severe("There was no data for %s level, but their total is %s. Check your bags.yml! (levels section)".formatted(level, section.getKeys(false).size()));
                break;
            }
            Level levelData = new Level(level, levelSection.getInt("capacity"), levelSection.getInt("slots"), levelSection.getInt("cost"), levelSection.getStringList("commands"));
            this.levels.put(level, levelData);
        }
        this.maxLevel = this.levels.size();
        this.levelsString = this.levels.keySet().stream().map(String::valueOf).toList();
    }

    public List<String> getLevelsString() {
        return this.levelsString;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    public Level getLevel(int level) {
        return (Level)this.levels.get(this.normalizeLevel(level));
    }

    public int normalizeLevel(int level) {
        return Math.max(1, Math.min(level, this.maxLevel));
    }

    public record Level(int level, int capacity, int slots, int cost, List<String> commands) {
        @Override
        public String toString() {
            return "Level{level=%s, capacity=%s, slots=%s, cost=%s}".formatted(this.level, this.capacity, this.slots, this.cost);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.level, this.capacity, this.slots, this.cost);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Level)) {
                return false;
            }
            Level other = (Level)obj;
            return this.level == other.level;
        }
    }
}

