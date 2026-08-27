package work.mcwws.ultimatetimberfix;

import com.songoda.ultimatetimber.UltimateTimber;
import com.songoda.ultimatetimber.manager.PlacedBlockManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

/**
 * ExoticGarden 会取消 {@code StructureGrowEvent}，导致 UltimateTimber 的
 * PlacedBlockManager 无法在生长时清掉「玩家放置」标记；树基（原树苗格）
 * 会一直被视为放置方块，连根砍检测直接失败。
 */
public final class UltimateTimberPlacedBlocks {

    private static final BlockFace[] SCAN_FACES = {
            BlockFace.UP, BlockFace.DOWN,
            BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST
    };

    private static Field placedBlocksField;
    private static boolean reflectionFailed;

    private UltimateTimberPlacedBlocks() {
    }

    public static boolean isMarkedPlaced(Block block) {
        if (block == null) {
            return false;
        }
        PlacedBlockManager manager = manager();
        if (manager == null) {
            return false;
        }
        try {
            return manager.isBlockPlaced(block);
        } catch (Throwable t) {
            return false;
        }
    }

    public static int clear(Block block) {
        if (block == null) {
            return 0;
        }
        return clear(block.getLocation());
    }

    public static int clear(Location location) {
        Set<Location> placed = placedSet();
        if (placed == null || location == null || location.getWorld() == null) {
            return 0;
        }
        Location key = blockKey(location);
        return placed.remove(key) ? 1 : 0;
    }

    /**
     * 从原点 BFS 树部件，清掉 UT 放置标记（树基 + 可能被误记的邻接格）。
     */
    public static int clearTreeAround(Block origin, int maxBlocks) {
        if (origin == null) {
            return 0;
        }
        Set<Location> placed = placedSet();
        if (placed == null || placed.isEmpty()) {
            return 0;
        }

        int removed = 0;
        Queue<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(origin);
        int limit = Math.max(16, maxBlocks);

        while (!queue.isEmpty() && visited.size() < limit) {
            Block block = queue.poll();
            String visitKey = TreeFootprintStore.key(block.getLocation());
            if (!visited.add(visitKey)) {
                continue;
            }
            if (!SlimefunTreeDetector.isTreePart(block.getType())
                    && block != origin) {
                continue;
            }
            if (placed.remove(blockKey(block.getLocation()))) {
                removed++;
            }
            for (BlockFace face : SCAN_FACES) {
                queue.add(block.getRelative(face));
            }
        }
        return removed;
    }

    private static Location blockKey(Location location) {
        // PlacedBlockManager 使用 Block#getLocation()，含 yaw/pitch=0
        return new Location(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    private static PlacedBlockManager manager() {
        UltimateTimber plugin = UltimateTimber.getInstance();
        if (plugin == null) {
            return null;
        }
        return plugin.getPlacedBlockManager();
    }

    @SuppressWarnings("unchecked")
    private static Set<Location> placedSet() {
        if (reflectionFailed) {
            return null;
        }
        PlacedBlockManager manager = manager();
        if (manager == null) {
            return null;
        }
        try {
            if (placedBlocksField == null) {
                placedBlocksField = PlacedBlockManager.class.getDeclaredField("placedBlocks");
                placedBlocksField.setAccessible(true);
            }
            Object value = placedBlocksField.get(manager);
            if (value instanceof Set<?> set) {
                return (Set<Location>) set;
            }
        } catch (Throwable t) {
            reflectionFailed = true;
            McwwsUltimateTimberFixPlugin plugin = McwwsUltimateTimberFixPlugin.getInstance();
            if (plugin != null) {
                plugin.getLogger().log(Level.WARNING, "无法访问 UltimateTimber PlacedBlockManager.placedBlocks", t);
            }
        }
        return null;
    }
}
