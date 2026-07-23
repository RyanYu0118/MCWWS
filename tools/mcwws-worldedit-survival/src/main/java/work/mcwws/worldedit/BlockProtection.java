package work.mcwws.worldedit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class BlockProtection {

    private BlockProtection() {
    }

    public static boolean shouldBypass(Player player) {
        if (player == null) {
            return true;
        }
        String perm = McwwsWeSurvivalPlugin.getInstance().getPluginConfig().getString("bypass-permission", "mcwws.we.survival.bypass");
        return player.hasPermission(perm) || player.hasPermission("fawe.bypass") || player.hasPermission("worldedit.bypass");
    }

    public static boolean isSurvivalLike(Player player) {
        if (player == null) {
            return false;
        }
        GameMode mode = player.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    public static boolean isProtectedWorldBlock(World world, BlockVector3 pos) {
        org.bukkit.World bukkitWorld = BukkitAdapter.adapt(world);
        if (bukkitWorld == null) {
            return false;
        }
        Location loc = new Location(bukkitWorld, pos.x(), pos.y(), pos.z());
        return isProtectedBukkitBlock(loc.getBlock());
    }

    public static boolean isProtectedBukkitBlock(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        if (type.isAir()) {
            return false;
        }
        if (isUnbreakable(type, block)) {
            return true;
        }
        try {
            if (BlockStorage.hasBlockInfo(block)) {
                return true;
            }
        } catch (Throwable ignored) {
            // Slimefun not loaded
        }
        return false;
    }

    private static boolean isUnbreakable(Material type, Block block) {
        if (type == Material.BEDROCK || type == Material.BARRIER || type == Material.END_PORTAL_FRAME
                || type == Material.END_PORTAL || type == Material.NETHER_PORTAL || type == Material.REINFORCED_DEEPSLATE
                || type == Material.COMMAND_BLOCK || type == Material.CHAIN_COMMAND_BLOCK
                || type == Material.REPEATING_COMMAND_BLOCK || type == Material.STRUCTURE_BLOCK
                || type == Material.JIGSAW || type == Material.LIGHT) {
            return true;
        }
        return type.getHardness() < 0F;
    }
}
