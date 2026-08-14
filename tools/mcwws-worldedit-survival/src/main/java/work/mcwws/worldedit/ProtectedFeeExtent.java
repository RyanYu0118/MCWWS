package work.mcwws.worldedit;

import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.entity.Player;

public final class ProtectedFeeExtent extends AbstractDelegateExtent {

    private final World world;
    private final Player player;

    public ProtectedFeeExtent(Extent extent, World world, Player player) {
        super(extent);
        this.world = world;
        this.player = player;
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 location, B block) throws com.sk89q.worldedit.WorldEditException {
        if (BlockProtection.isProtectedWorldBlock(world, location)) {
            return false;
        }
        if (player != null && !ResidenceProtection.canChange(player, world, location, block)) {
            return false;
        }
        if (player != null && !BlockProtection.shouldBypass(player) && !WeEditAuthorization.tryConsumeBlock(player)) {
            return false;
        }
        return super.setBlock(location, block);
    }
}
