package work.mcwws.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.entity.Player;

/**
 * 预扫描读方块：Bukkit + FAWE 队列 + 上一笔扣费选区记忆 + EditSession 回退。
 */
final class EstimateBlockReader {

    private static volatile Boolean fawePresent;

    private EstimateBlockReader() {
    }

    static BlockState readForTarget(World world, BlockVector3 pos, BaseBlock target) {
        BlockState bukkit = BukkitSnapshotExtent.readBlock(world, pos);
        if (target == null) {
            return bukkit;
        }
        BaseBlock bukkitBase = bukkit.toBaseBlock();
        if (bukkitBase.equals(target)) {
            Player player = EstimateContext.player();
            BlockState recent = WeRecentEditMemory.resolveStaleTargetRead(player, world, pos, bukkit);
            if (recent != null && !recent.toBaseBlock().equals(target)) {
                return recent;
            }
            BlockState session = readEditSession(world, pos);
            if (session != null && !session.toBaseBlock().equals(target)) {
                return session;
            }
        }
        BlockState fawe = readFaweQueue(world, pos);
        if (fawe == null || fawe.equals(bukkit)) {
            return bukkit;
        }
        BaseBlock faweBase = fawe.toBaseBlock();
        if (bukkitBase.equals(target) && !faweBase.equals(target)) {
            return fawe;
        }
        if (faweBase.equals(target) && !bukkitBase.equals(target)) {
            return bukkit;
        }
        return bukkit;
    }

    static BlockState readForFromFilter(World world, BlockVector3 pos, BaseBlock fromHint) {
        BlockState bukkit = BukkitSnapshotExtent.readBlock(world, pos);
        if (fromHint == null) {
            return bukkit;
        }
        BlockState fawe = readFaweQueue(world, pos);
        if (fawe == null || fawe.equals(bukkit)) {
            return bukkit;
        }
        BaseBlock bukkitBase = bukkit.toBaseBlock();
        BaseBlock faweBase = fawe.toBaseBlock();
        if (faweBase.equals(fromHint) && !bukkitBase.equals(fromHint)) {
            return fawe;
        }
        if (bukkitBase.equals(fromHint) && !faweBase.equals(fromHint)) {
            return bukkit;
        }
        return bukkit;
    }

    private static BlockState readEditSession(World world, BlockVector3 pos) {
        try {
            EditSession session = WorldEdit.getInstance().newEditSession(world);
            try {
                return session.getBlock(pos);
            } finally {
                session.close();
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static BlockState readFaweQueue(World world, BlockVector3 pos) {
        if (!isFawePresent()) {
            return null;
        }
        try {
            com.fastasyncworldedit.core.queue.IQueueExtent<?> queue =
                    com.fastasyncworldedit.core.FaweAPI.createQueue(world, true);
            return queue.getBlock(pos.x(), pos.y(), pos.z());
        } catch (Throwable ignored) {
            return null;
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
