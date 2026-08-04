package work.mcwws.worldedit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;

/**
 * 预扫描用：从 Bukkit 世界读取当前已落盘方块，避免 FAWE 队列/缓存导致
 * 「上一笔刚改完、扩大选区后误判 0 格变更」从而取消扣费但 FAWE 仍执行。
 */
final class BukkitSnapshotExtent extends AbstractDelegateExtent {

    private final World world;

    BukkitSnapshotExtent(World world) {
        super(world);
        this.world = world;
    }

    static Extent forEstimate(World world) {
        return new BukkitSnapshotExtent(world);
    }

    static BlockState readBlock(World world, BlockVector3 pos) {
        org.bukkit.World bukkit = BukkitAdapter.adapt(world);
        if (bukkit == null) {
            return world.getBlock(pos);
        }
        org.bukkit.block.Block block = bukkit.getBlockAt(pos.x(), pos.y(), pos.z());
        return BukkitAdapter.adapt(block.getBlockData());
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return readBlock(world, position);
    }
}
