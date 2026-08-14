package work.mcwws.worldedit;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.listeners.ResidenceBlockListener;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * 批量编辑的领地闸门：只约束「落在领地内」的格子。
 * 纯拆除走 Residence 官方破坏检查；非空气目标要求官方放置与破坏检查同时通过。
 */
final class ResidenceProtection {

    private ResidenceProtection() {
    }

    static boolean canChange(Player player, World world, BlockVector3 position, BlockStateHolder<?> target) {
        if (player == null || world == null || position == null || target == null) {
            return false;
        }
        org.bukkit.World bukkitWorld = BukkitAdapter.adapt(world);
        if (bukkitWorld == null) {
            return false;
        }
        Location location = new Location(bukkitWorld, position.x(), position.y(), position.z());
        boolean demolitionOnly = target.getBlockType().getMaterial().isAir();
        return canChange(player, location, demolitionOnly);
    }

    private static boolean canChange(Player player, Location location, boolean demolitionOnly) {
        Residence residence = Residence.getInstance();
        if (residence == null || !residence.isEnabled()) {
            return true;
        }
        ClaimedResidence claim = residence.getResidenceManager().getByLoc(location);
        if (claim == null) {
            // 用户规则只约束领地内；荒野不拦
            return true;
        }
        Block block = location.getBlock();
        if (demolitionOnly) {
            return ResidenceBlockListener.canBreakBlock(player, block, false);
        }
        return ResidenceBlockListener.canBreakBlock(player, block, false)
                && ResidenceBlockListener.canPlaceBlock(player, block, false);
    }
}
