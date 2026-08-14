package work.mcwws.axiomsurvival;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/**
 * 让 Axiom 批量编辑遵守 Residence 与玩家手动放置/破坏相同的权限。
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
            return true;
        }

        FlagPermissions permissions = residence.getPermsByLocForPlayer(location, player);
        boolean build = permissions.playerHas(player, Flags.build, true);
        boolean destroy = permissions.playerHas(player, Flags.destroy, build);
        if (target.getMaterial().isAir()) {
            return destroy;
        }
        boolean place = permissions.playerHas(player, Flags.place, build);
        return destroy && place;
    }
}
