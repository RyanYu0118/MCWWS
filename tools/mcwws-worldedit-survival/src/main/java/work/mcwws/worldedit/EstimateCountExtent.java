package work.mcwws.worldedit;

import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * 预扫描用：与 FAWE 写块对齐，但不写世界，只统计会变更的格子。
 * 必须走 {@link #setBlock} 计数，不能依赖 {@code Pattern.apply}（其会按 Bukkit 视图短路跳过 setBlock）。
 * {@code rememberWrites} 时后续 {@code getBlock} 能看到本轮已模拟的改块（搬运/重力/生成实心体需要）。
 */
final class EstimateCountExtent extends AbstractDelegateExtent {

    private final World world;
    private final FeeEstimate.ResultBuilder builder;
    private final Map<BlockVector3, BaseBlock> overlay;

    EstimateCountExtent(World world, FeeEstimate.ResultBuilder builder) {
        this(world, builder, false);
    }

    EstimateCountExtent(World world, FeeEstimate.ResultBuilder builder, boolean rememberWrites) {
        super(BukkitSnapshotExtent.forEstimate(world));
        this.world = world;
        this.builder = builder;
        this.overlay = rememberWrites ? new HashMap<>() : null;
    }

    static Extent forEstimate(World world, FeeEstimate.ResultBuilder builder) {
        return new EstimateCountExtent(world, builder);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        if (overlay != null) {
            BaseBlock simulated = overlay.get(position);
            if (simulated != null) {
                return simulated.toImmutableState();
            }
        }
        return BukkitSnapshotExtent.readBlock(world, position);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        if (overlay != null) {
            BaseBlock simulated = overlay.get(position);
            if (simulated != null) {
                return simulated;
            }
        }
        return getBlock(position).toBaseBlock();
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
        if (!ResidenceProtection.canChange(EstimateContext.player(), world, location, after)) {
            builder.residenceDeniedBlocks++;
            return false;
        }
        BlockState existing;
        if (overlay != null && overlay.containsKey(location)) {
            existing = overlay.get(location).toImmutableState();
        } else {
            existing = EstimateBlockReader.readForTarget(world, location, after);
        }
        if (existing == null) {
            return false;
        }
        BaseBlock before = existing.toBaseBlock();
        if (before.equals(after)) {
            return false;
        }
        builder.addChange(before, after);
        if (overlay != null) {
            overlay.put(location, after);
        }
        return true;
    }
}
