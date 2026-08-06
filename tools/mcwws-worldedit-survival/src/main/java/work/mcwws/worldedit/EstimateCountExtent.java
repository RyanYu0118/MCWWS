package work.mcwws.worldedit;

import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

/**
 * 预扫描用：与 FAWE //set / //replace 相同路径（Pattern.apply → setBlock），
 * 但不写世界，只统计会变更的格子并累加费用。
 */
final class EstimateCountExtent extends AbstractDelegateExtent {

    private final World world;
    private final FeeEstimate.ResultBuilder builder;

    EstimateCountExtent(World world, FeeEstimate.ResultBuilder builder) {
        super(BukkitSnapshotExtent.forEstimate(world));
        this.world = world;
        this.builder = builder;
    }

    static Extent forEstimate(World world, FeeEstimate.ResultBuilder builder) {
        return new EstimateCountExtent(world, builder);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return BukkitSnapshotExtent.readBlock(world, position);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) {
        if (BlockProtection.isProtectedWorldBlock(world, location)) {
            builder.protectedBlocks++;
            return false;
        }
        BlockState existing = getBlock(location);
        if (existing == null || block == null) {
            return false;
        }
        BaseBlock before = existing.toBaseBlock();
        BaseBlock after = block.toBaseBlock();
        if (before.equals(after)) {
            return false;
        }
        builder.addChange(before, after);
        return true;
    }
}
