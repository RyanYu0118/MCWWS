package work.mcwws.worldedit;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 让批量编辑遵守 Residence 与玩家手动放置/破坏相同的权限。
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
            return true;
        }

        FlagPermissions permissions = residence.getPermsByLocForPlayer(location, player);
        boolean build = permissions.playerHas(player, Flags.build, true);
        boolean destroy = permissions.playerHas(player, Flags.destroy, build);
        if (demolitionOnly) {
            return destroy;
        }
        boolean place = permissions.playerHas(player, Flags.place, build);
        return destroy && place;
    }
}
