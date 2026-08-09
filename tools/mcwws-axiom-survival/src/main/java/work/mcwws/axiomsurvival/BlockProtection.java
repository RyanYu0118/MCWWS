package work.mcwws.axiomsurvival;

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
        String perm = McwwsAxiomSurvivalPlugin.getInstance().getPluginConfig()
                .getString("bypass-permission", "mcwws.axiom.survival.bypass");
        return player.hasPermission(perm);
    }

    public static boolean isSurvivalLike(Player player) {
        if (player == null) {
            return false;
        }
        GameMode mode = player.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    public static boolean isProtectedBlock(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        if (type.isAir()) {
            return false;
        }
        if (isUnbreakable(type)) {
            return true;
        }
        try {
            if (BlockStorage.hasBlockInfo(block)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean isProtectedLocation(Location location) {
        return location != null && isProtectedBlock(location.getBlock());
    }

    private static boolean isUnbreakable(Material type) {
        return type == Material.BEDROCK || type == Material.BARRIER || type == Material.END_PORTAL_FRAME
                || type == Material.END_PORTAL || type == Material.NETHER_PORTAL || type == Material.REINFORCED_DEEPSLATE
                || type == Material.COMMAND_BLOCK || type == Material.CHAIN_COMMAND_BLOCK
                || type == Material.REPEATING_COMMAND_BLOCK || type == Material.STRUCTURE_BLOCK
                || type == Material.JIGSAW || type == Material.LIGHT;
    }
}
