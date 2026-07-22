/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.jetbrains.annotations.NotNull
 */
package cat.necko.bags.config.items;

import cat.necko.bags.Plugin;
import cat.necko.bags.config.items.ItemsFile;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public class ItemsData {
    private final ConcurrentMap<Material, Integer> items;
    private final Set<Material> sellable;

    public ItemsData(ItemsFile itemsFile) {
        FileConfiguration config = itemsFile.getConfig();
        ConfigurationSection items = config.getConfigurationSection("items");
        assert (items != null);
        this.items = new ConcurrentHashMap<Material, Integer>();
        this.sellable = new HashSet<Material>();
        items.getKeys(false).forEach(type -> {
            Material material;
            try {
                material = Material.valueOf((String)type);
            }
            catch (IllegalArgumentException ignored) {
                Plugin.getInstance().getLogger().warning("Unknown material %s".formatted(type));
                return;
            }
            int cost = items.getInt(type);
            if (this.sellable.add(material)) {
                this.items.put(material, cost);
            }
        });
    }

    @NotNull
    public Integer getCost(@NotNull Material material) {
        if (material == null) {
            ItemsData.$$$reportNull$$$0(0);
        }
        Integer n = this.items.getOrDefault(material, 0);
        if (n == null) {
            ItemsData.$$$reportNull$$$0(1);
        }
        return n;
    }

    public boolean isSellable(@NotNull Material material) {
        if (material == null) {
            ItemsData.$$$reportNull$$$0(2);
        }
        return this.sellable.contains(material);
    }

    public Set<Material> getSellable() {
        return this.sellable;
    }

    private static /* synthetic */ void $$$reportNull$$$0(int n) {
        Object[] objectArray;
        Object[] objectArray2;
        Object[] objectArray3 = new Object[switch (n) {
            default -> 3;
            case 1 -> 2;
        }];
        switch (n) {
            default: {
                objectArray2 = objectArray3;
                objectArray3[0] = "material";
                break;
            }
            case 1: {
                objectArray2 = objectArray3;
                objectArray3[0] = "cat/necko/bags/config/items/ItemsData";
                break;
            }
        }
        switch (n) {
            default: {
                objectArray = objectArray2;
                objectArray2[1] = "cat/necko/bags/config/items/ItemsData";
                break;
            }
            case 1: {
                objectArray = objectArray2;
                objectArray2[1] = "getCost";
                break;
            }
        }
        switch (n) {
            default: {
                objectArray = objectArray;
                objectArray[2] = "getCost";
                break;
            }
            case 1: {
                break;
            }
            case 2: {
                objectArray = objectArray;
                objectArray[2] = "isSellable";
                break;
            }
        }
        String string = String.format(v0, objectArray);
        throw switch (n) {
            default -> new IllegalArgumentException(string);
            case 1 -> new IllegalStateException(string);
        };
    }
}

