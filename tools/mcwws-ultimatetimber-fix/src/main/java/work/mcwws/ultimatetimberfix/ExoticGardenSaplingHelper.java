package work.mcwws.ultimatetimberfix;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class ExoticGardenSaplingHelper {

    private ExoticGardenSaplingHelper() {
    }

    public static ItemStack createSaplingItem(String saplingId) {
        SlimefunItem slimefunItem = SlimefunItem.getById(saplingId);
        if (slimefunItem == null) {
            return null;
        }
        return slimefunItem.getItem().clone();
    }

    public static void dropSaplingsForLeaves(McwwsUltimateTimberFixPlugin plugin, FellSession session, Player player) {
        ItemStack saplingItem = createSaplingItem(session.getSaplingId());
        if (saplingItem == null || session.getLeafKeys().isEmpty()) {
            return;
        }

        double chance = plugin.saplingDropChancePerLeaf() / 100.0D;
        Random random = ThreadLocalRandom.current();
        for (String leafKey : session.getLeafKeys()) {
            Location location = TreeFootprintStore.parseKey(leafKey);
            if (location == null) {
                continue;
            }
            if (random.nextDouble() > chance) {
                continue;
            }
            location.getWorld().dropItemNaturally(location.add(0.5D, 0.5D, 0.5D), saplingItem.clone());
        }
    }

    public static void scheduleReplant(McwwsUltimateTimberFixPlugin plugin, FellSession session) {
        ItemStack saplingItem = createSaplingItem(session.getSaplingId());
        if (saplingItem == null) {
            return;
        }

        Location base = session.getBaseLocation();
        List<Integer> delays = plugin.replantDelaysTicks();
        for (int delay : delays) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plantSapling(base, saplingItem);
            }, delay);
        }
    }

    public static void plantSapling(Location base, ItemStack saplingItem) {
        if (base == null || base.getWorld() == null || saplingItem == null) {
            return;
        }
        Block block = base.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        if (!isPlantableSoil(below.getType())) {
            return;
        }
        if (!block.getType().isAir() && !Tag.SAPLINGS.isTagged(block.getType())) {
            return;
        }

        block.setType(saplingItem.getType(), false);
        // 补种会按多个延迟重试，重复 store 会让 Slimefun 抛 "There already a block in this location"
        try {
            if (BlockStorage.hasBlockInfo(block)) {
                BlockStorage.clearBlockInfo(block);
            }
            BlockStorage.store(block, saplingItem);
        } catch (Throwable ignored) {
        }
    }

    public static boolean isLeafOrVanillaSapling(Material material) {
        return material != null && (Tag.LEAVES.isTagged(material) || Tag.SAPLINGS.isTagged(material));
    }

    private static boolean isPlantableSoil(Material material) {
        return material == Material.GRASS_BLOCK
                || material == Material.DIRT
                || material == Material.COARSE_DIRT
                || material == Material.PODZOL
                || material == Material.ROOTED_DIRT;
    }
}
