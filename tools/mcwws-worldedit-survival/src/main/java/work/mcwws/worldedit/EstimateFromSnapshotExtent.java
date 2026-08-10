package work.mcwws.worldedit;

import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

/** //replace 预扫描：from 掩码读块时合并 FAWE 队列，避免连续编辑后 Bukkit 滞后。 */
final class EstimateFromSnapshotExtent extends AbstractDelegateExtent {

    private final World world;
    private final Pattern fromPattern;

    EstimateFromSnapshotExtent(World world, Pattern fromPattern) {
        super(BukkitSnapshotExtent.forEstimate(world));
        this.world = world;
        this.fromPattern = fromPattern;
    }

    static Extent forReplaceFrom(World world, Pattern fromPattern) {
        return new EstimateFromSnapshotExtent(world, fromPattern);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        if (fromPattern == null) {
            return BukkitSnapshotExtent.readBlock(world, position);
        }
        BaseBlock fromHint = fromPattern.applyBlock(position);
        return EstimateBlockReader.readForFromFilter(world, position, fromHint);
    }
}
