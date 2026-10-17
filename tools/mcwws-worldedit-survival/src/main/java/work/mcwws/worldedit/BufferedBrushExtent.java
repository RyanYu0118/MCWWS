package work.mcwws.worldedit;

import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.LinkedHashMap;
import java.util.Map;

/** Capture one brush stroke after masks/transforms, before history/queue writes.
 * Random patterns run only once. Nothing reaches the world until billing approves.
 */
class BufferedBrushExtent extends AbstractDelegateExtent {
    record Change(BaseBlock before, BaseBlock after) { }

    @FunctionalInterface
    interface Approval {
        // May remove protected blocks. False rejects the entire stroke.
        boolean approve(Map<BlockVector3, Change> changes, boolean tooLarge);
    }

    private final Map<BlockVector3, Change> changes = new LinkedHashMap<>();
    private final long maxBlocks;
    private final Approval approval;
    private boolean tooLarge;
    private boolean finished;

    BufferedBrushExtent(Extent extent, long maxBlocks, Approval approval) {
        super(extent);
        this.maxBlocks = Math.max(1, maxBlocks);
        this.approval = approval;
    }

    @Override
    public synchronized BaseBlock getFullBlock(BlockVector3 pos) {
        Change change = changes.get(pos);
        return change == null ? super.getFullBlock(pos) : change.after();
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        return getFullBlock(BlockVector3.at(x, y, z));
    }

    @Override
    public BlockState getBlock(BlockVector3 pos) {
        return getFullBlock(pos).toImmutableState();
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        return getBlock(BlockVector3.at(x, y, z));
    }

    @Override
    public synchronized <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 pos, B block) {
        if (finished || tooLarge || block == null) {
            return false;
        }
        BaseBlock after = block.toBaseBlock();
        Change previous = changes.get(pos);
        BaseBlock current = previous == null ? super.getFullBlock(pos) : previous.after();
        if (current.equals(after)) {
            return false;
        }
        BaseBlock before = previous == null ? current : previous.before();
        if (before.equals(after)) {
            changes.remove(pos);
        } else {
            // FAWE may reuse mutable vector instances while iterating.
            changes.put(BlockVector3.at(pos.x(), pos.y(), pos.z()), new Change(before, after));
        }
        if (changes.size() > maxBlocks) {
            tooLarge = true;
            changes.clear();
            return false;
        }
        return true;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(int x, int y, int z, B block) {
        return setBlock(BlockVector3.at(x, y, z), block);
    }

    @Override
    protected synchronized Operation commitBefore() {
        if (finished) {
            return null;
        }
        finished = true;
        try {
            if ((tooLarge || !changes.isEmpty()) && approval.approve(changes, tooLarge)) {
                for (var entry : changes.entrySet()) {
                    super.setBlock(entry.getKey(), entry.getValue().after());
                }
            }
        } catch (com.sk89q.worldedit.WorldEditException ex) {
            throw new IllegalStateException("Brush commit failed", ex);
        } finally {
            changes.clear();
        }
        return null;
    }
}
