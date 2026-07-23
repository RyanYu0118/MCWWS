package work.mcwws.worldedit;

import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public final class ProtectedFeeExtent extends AbstractDelegateExtent {

    private final World world;

    public ProtectedFeeExtent(Extent extent, World world) {
        super(extent);
        this.world = world;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws com.sk89q.worldedit.WorldEditException {
        if (BlockProtection.isProtectedWorldBlock(world, location)) {
            return false;
        }
        return super.setBlock(location, block);
    }
}
