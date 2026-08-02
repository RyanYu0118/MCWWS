package work.mcwws.worldedit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import org.bukkit.Chunk;

public final class RegionChunkLoader {

    private RegionChunkLoader() {
    }

    static void ensureLoaded(World world, Region region) {
        if (world == null || region == null) {
            return;
        }
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        ensureBoxLoaded(world, min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }

    static void ensureLoaded(World world, BlockVector3 center, int radius) {
        if (world == null || center == null || radius < 0) {
            return;
        }
        ensureBoxLoaded(
                world,
                center.x() - radius,
                center.y() - radius,
                center.z() - radius,
                center.x() + radius,
                center.y() + radius,
                center.z() + radius
        );
    }

    static void ensureLoadedForStack(World world, Region region, BlockVector3 blockOffset, int count) {
        if (world == null || region == null || blockOffset == null || count < 1) {
            return;
        }
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        BlockVector3 total = blockOffset.multiply(count);
        int minX = min.x() + Math.min(blockOffset.x(), total.x());
        int minY = min.y() + Math.min(blockOffset.y(), total.y());
        int minZ = min.z() + Math.min(blockOffset.z(), total.z());
        int maxX = max.x() + Math.max(blockOffset.x(), total.x());
        int maxY = max.y() + Math.max(blockOffset.y(), total.y());
        int maxZ = max.z() + Math.max(blockOffset.z(), total.z());
        ensureBoxLoaded(world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void ensureBoxLoaded(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        org.bukkit.World bukkit = BukkitAdapter.adapt(world);
        if (bukkit == null) {
            return;
        }
        int minCx = minX >> 4;
        int maxCx = maxX >> 4;
        int minCz = minZ >> 4;
        int maxCz = maxZ >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                Chunk chunk = bukkit.getChunkAt(cx, cz);
                if (!chunk.isLoaded()) {
                    chunk.load(true);
                }
            }
        }
    }
}
