package work.mcwws.ultimatetimberfix;

import com.songoda.ultimatetimber.tree.DetectedTree;
import com.songoda.ultimatetimber.tree.ITreeBlock;
import com.songoda.ultimatetimber.tree.TreeBlockType;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;

public final class SlimefunTreeDetector {

    private SlimefunTreeDetector() {
    }

    public static boolean isFruitTree(DetectedTree tree) {
        return resolveSaplingId(tree) != null;
    }

    public static String resolveSaplingId(DetectedTree tree) {
        if (tree == null) {
            return null;
        }
        var blocks = tree.getDetectedTreeBlocks();
        if (blocks == null) {
            return null;
        }

        for (ITreeBlock<?> treeBlock : blocks.getAllTreeBlocks()) {
            Object raw = treeBlock.getBlock();
            if (!(raw instanceof Block block)) {
                continue;
            }
            String saplingId = resolveSaplingId(block);
            if (saplingId != null) {
                return saplingId;
            }
        }
        return null;
    }

    public static String resolveSaplingId(Block block) {
        if (block == null) {
            return null;
        }
        String slimefunId = slimefunIdAt(block);
        String mapped = ExoticGardenRegistry.toSaplingId(slimefunId);
        if (mapped != null) {
            return mapped;
        }
        Material type = block.getType();
        if ((type == Material.PLAYER_HEAD || type == Material.PLAYER_WALL_HEAD) && BlockStorage.hasBlockInfo(block)) {
            return ExoticGardenRegistry.toSaplingId(BlockStorage.checkID(block));
        }
        return null;
    }

    public static boolean isFruitTreeBlock(Block block) {
        return resolveSaplingId(block) != null;
    }

    public static boolean isFruitTreeSaplingId(String id) {
        return ExoticGardenRegistry.isSaplingId(id);
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

    public static boolean isLeafBlock(ITreeBlock<?> treeBlock) {
        return treeBlock != null && treeBlock.getTreeBlockType() == TreeBlockType.LEAF;
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
}
