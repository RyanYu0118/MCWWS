package work.mcwws.worldedit;

import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

/**
 * 预扫描前将 FAWE 队列中尚未落盘的改块同步到 Bukkit，避免连续 //set 时
 * Bukkit 仍显示上一笔之前的方块而误判 0 格变更。
 */
final class FaweRegionSync {

    private static volatile Boolean fawePresent;

    private FaweRegionSync() {
    }

    static void flushBeforeEstimate(World world, Region region) {
        if (world == null || region == null || !isFawePresent()) {
            return;
        }
        RegionChunkLoader.ensureLoaded(world, region);
        try {
            com.fastasyncworldedit.core.queue.IQueueExtent<?> queue =
                    com.fastasyncworldedit.core.FaweAPI.createQueue(world, false);
            if (queue != null) {
                queue.flush();
            }
        } catch (Throwable ignored) {
            // 无 FAWE 或 flush 失败时仍回退为直接读 Bukkit
        }
    }

    static void flushBeforeEstimate(World world, com.sk89q.worldedit.math.BlockVector3 center, int radius) {
        if (world == null || center == null || radius < 0 || !isFawePresent()) {
            return;
        }
        RegionChunkLoader.ensureLoaded(world, center, radius);
        try {
            com.fastasyncworldedit.core.queue.IQueueExtent<?> queue =
                    com.fastasyncworldedit.core.FaweAPI.createQueue(world, false);
            if (queue != null) {
                queue.flush();
            }
        } catch (Throwable ignored) {
        }
    }

    static void flushBeforeEstimate(World world, com.sk89q.worldedit.math.BlockVector3 min, com.sk89q.worldedit.math.BlockVector3 max) {
        if (world == null || min == null || max == null || !isFawePresent()) {
            return;
        }
        RegionChunkLoader.ensureLoaded(world, min, max);
        try {
            com.fastasyncworldedit.core.queue.IQueueExtent<?> queue =
                    com.fastasyncworldedit.core.FaweAPI.createQueue(world, false);
            if (queue != null) {
                queue.flush();
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isFawePresent() {
        if (fawePresent == null) {
            try {
                Class.forName("com.fastasyncworldedit.core.FaweAPI");
                fawePresent = true;
            } catch (ClassNotFoundException ex) {
                fawePresent = false;
            }
        }
        return fawePresent;
    }
}
