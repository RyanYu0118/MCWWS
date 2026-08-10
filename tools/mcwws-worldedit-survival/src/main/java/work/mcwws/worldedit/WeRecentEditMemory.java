package work.mcwws.worldedit;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录上一笔已扣费改块的选区与放置物，用于连续 //set 时 Bukkit/FAWE 读块滞后、
 * 误判「已是目标方块」而 0 格变更的场景。
 */
final class WeRecentEditMemory {

    private static final long TTL_MS = 120_000L;

    private record Snapshot(
            String worldName,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            long timeMs,
            String dominantPlacedId
    ) {
        boolean recent() {
            return System.currentTimeMillis() - timeMs <= TTL_MS;
        }

        boolean contains(BlockVector3 pos) {
            return pos.x() >= minX && pos.x() <= maxX
                    && pos.y() >= minY && pos.y() <= maxY
                    && pos.z() >= minZ && pos.z() <= maxZ;
        }

        BlockState dominantPlacedState() {
            if (dominantPlacedId == null || dominantPlacedId.isBlank() || "air".equals(dominantPlacedId)) {
                return null;
            }
            var type = BlockTypes.get(dominantPlacedId);
            return type != null ? type.getDefaultState() : null;
        }
    }

    private static final ConcurrentHashMap<UUID, Snapshot> LAST = new ConcurrentHashMap<>();

    private WeRecentEditMemory() {
    }

    static void record(Player player, World world, Region region, Map<String, Long> placedCounts) {
        if (player == null || world == null || region == null || placedCounts == null || placedCounts.isEmpty()) {
            return;
        }
        String dominant = dominantPlacedId(placedCounts);
        if (dominant == null) {
            return;
        }
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        store(player, world.getName(), min, max, dominant);
    }

    static void recordBox(Player player, World world, BlockVector3 center, int radius, Map<String, Long> placedCounts) {
        if (player == null || world == null || center == null || radius < 0) {
            return;
        }
        String dominant = dominantPlacedId(placedCounts);
        if (dominant == null) {
            return;
        }
        BlockVector3 min = center.add(-radius, -radius, -radius);
        BlockVector3 max = center.add(radius, radius, radius);
        store(player, world.getName(), min, max, dominant);
    }

    static BlockState resolveStaleTargetRead(Player player, World world, BlockVector3 pos, BlockState bukkitState) {
        if (player == null || world == null || pos == null || bukkitState == null) {
            return null;
        }
        Snapshot snap = LAST.get(player.getUniqueId());
        if (snap == null || !snap.recent() || !world.getName().equals(snap.worldName()) || !snap.contains(pos)) {
            return null;
        }
        BlockState placed = snap.dominantPlacedState();
        if (placed == null || placed.equals(bukkitState)) {
            return null;
        }
        return placed;
    }

    private static String dominantPlacedId(Map<String, Long> placedCounts) {
        String dominant = null;
        long best = 0L;
        for (Map.Entry<String, Long> entry : placedCounts.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0L) {
                continue;
            }
            String id = entry.getKey();
            if (id == null || id.isBlank() || "air".equals(id)) {
                continue;
            }
            if (entry.getValue() > best) {
                best = entry.getValue();
                dominant = id;
            }
        }
        return dominant;
    }

    private static void store(Player player, String worldName, BlockVector3 min, BlockVector3 max, String dominant) {
        LAST.put(player.getUniqueId(), new Snapshot(
                worldName,
                min.x(), min.y(), min.z(),
                max.x(), max.y(), max.z(),
                System.currentTimeMillis(),
                dominant
        ));
    }
}
