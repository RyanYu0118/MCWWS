package work.mcwws.ultimatetimberfix;

import com.songoda.ultimatetimber.tree.DetectedTree;
import com.songoda.ultimatetimber.tree.ITreeBlock;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;

public final class SlimefunTreeDetector {

    private static final String EXOTIC_GARDEN_PREFIX = "EXOTIC_GARDEN_";

    private SlimefunTreeDetector() {
    }

    public static boolean isProtectedTree(DetectedTree tree) {
        if (tree == null) {
            return false;
        }
        var blocks = tree.getDetectedTreeBlocks();
        if (blocks == null) {
            return false;
        }
        for (ITreeBlock<?> treeBlock : blocks.getAllTreeBlocks()) {
            Object raw = treeBlock.getBlock();
            if (raw instanceof Block block && isProtectedBlock(block)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isProtectedBlock(Block block) {
        if (block == null) {
            return false;
        }
        if (isExoticGardenId(slimefunIdAt(block))) {
            return true;
        }
        Material type = block.getType();
        if ((type == Material.PLAYER_HEAD || type == Material.PLAYER_WALL_HEAD) && BlockStorage.hasBlockInfo(block)) {
            return isExoticGardenId(BlockStorage.checkID(block));
        }
        return false;
    }

    public static boolean isTreePart(Material type) {
        if (type == null || type.isAir()) {
            return false;
        }
        return Tag.LOGS.isTagged(type)
                || Tag.LEAVES.isTagged(type)
                || type == Material.PLAYER_HEAD
                || type == Material.PLAYER_WALL_HEAD
                || Tag.SAPLINGS.isTagged(type);
    }

    public static String slimefunIdAt(Block block) {
        if (block == null) {
            return null;
        }
        try {
            if (BlockStorage.hasBlockInfo(block)) {
                String id = BlockStorage.checkID(block);
                if (id != null) {
                    return id;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            SlimefunItem item = StorageCacheUtils.getSfItem(block.getLocation());
            return item != null ? item.getId() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isExoticGardenSaplingId(String id) {
        return isExoticGardenId(id) && id.contains("SAPLING");
    }

    public static boolean isExoticGardenId(String id) {
        return id != null && id.startsWith(EXOTIC_GARDEN_PREFIX);
    }
}
