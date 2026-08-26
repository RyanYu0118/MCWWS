package work.mcwws.ultimatetimberfix;

import io.github.thebusybiscuit.exoticgarden.ExoticGarden;
import io.github.thebusybiscuit.exoticgarden.Tree;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ExoticGardenRegistry {

    private static final Map<String, String> ID_TO_SAPLING = new HashMap<>();
    private static final Set<String> FRUIT_TREE_IDS = new HashSet<>();

    private ExoticGardenRegistry() {
    }

    public static void reload() {
        ID_TO_SAPLING.clear();
        FRUIT_TREE_IDS.clear();

        Plugin plugin = Bukkit.getPluginManager().getPlugin("ExoticGarden");
        if (!(plugin instanceof ExoticGarden exoticGarden)) {
            return;
        }

        for (Tree tree : exoticGarden.getTrees()) {
            String sapling = normalize(tree.getSapling());
            String fruit = normalize(tree.getFruitID());
            if (sapling == null) {
                continue;
            }
            ID_TO_SAPLING.put(sapling, sapling);
            FRUIT_TREE_IDS.add(sapling);
            if (fruit != null) {
                ID_TO_SAPLING.put(fruit, sapling);
                FRUIT_TREE_IDS.add(fruit);
            }
        }
    }

    public static boolean isFruitTreeId(String id) {
        return id != null && FRUIT_TREE_IDS.contains(normalize(id));
    }

    public static boolean isSaplingId(String id) {
        if (id == null) {
            return false;
        }
        String normalized = normalize(id);
        return normalized.endsWith("_SAPLING") && FRUIT_TREE_IDS.contains(normalized);
    }

    public static String toSaplingId(String slimefunId) {
        if (slimefunId == null) {
            return null;
        }
        return ID_TO_SAPLING.get(normalize(slimefunId));
    }

    private static String normalize(String id) {
        return id == null ? null : id.toUpperCase(Locale.ROOT);
    }
}
