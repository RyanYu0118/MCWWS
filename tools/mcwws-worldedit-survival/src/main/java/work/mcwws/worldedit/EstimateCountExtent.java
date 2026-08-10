package work.mcwws.worldedit;

import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

/**
 * 预扫描用：与 FAWE //set / //replace 对齐，但不写世界，只统计会变更的格子。
 * 必须走 {@link #setBlock} 计数，不能依赖 {@code Pattern.apply}（其会按 Bukkit 视图短路跳过 setBlock）。
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
        if (block == null) {
            return false;
        }
        BaseBlock after = block.toBaseBlock();
        BlockState existing = EstimateBlockReader.readForTarget(world, location, after);
        if (existing == null) {
            return false;
        }
        BaseBlock before = existing.toBaseBlock();
        if (before.equals(after)) {
            return false;
        }
        builder.addChange(before, after);
        return true;
    }
}
