package work.mcwws.axiomsurvival;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.listeners.ResidenceBlockListener;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/**
 * 批量编辑的领地闸门：只约束「落在领地内」的格子。
 * 纯拆除走 Residence 官方破坏检查；非空气目标要求官方放置与破坏检查同时通过。
 */
final class ResidenceProtection {

    private ResidenceProtection() {
    }

    static boolean canChange(Player player, Block block, BlockData target) {
        if (player == null || block == null || target == null) {
            return false;
        }
        Residence residence = Residence.getInstance();
        if (residence == null || !residence.isEnabled()) {
            return true;
        }

        Location location = block.getLocation();
        ClaimedResidence claim = residence.getResidenceManager().getByLoc(location);
        if (claim == null) {
            // 用户规则只约束领地内；荒野不拦
            return true;
        }
        if (target.getMaterial().isAir()) {
            return ResidenceBlockListener.canBreakBlock(player, block, false);
        }
        return ResidenceBlockListener.canBreakBlock(player, block, false)
                && ResidenceBlockListener.canPlaceBlock(player, block, false);
    }
}
