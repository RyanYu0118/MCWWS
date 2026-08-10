package work.mcwws.axiomsurvival;

import me.xpyex.plugin.flywithfood.common.implementation.FWFUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class FlyWithFoodBridge {

    private FlyWithFoodBridge() {
    }

    static boolean available() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("FlyWithFood");
        return plugin != null && plugin.isEnabled();
    }

    static boolean enableFly(Player player) {
        if (player == null) {
            return false;
        }
        if (available()) {
            try {
                FWFUser user = FWFUser.of(player.getName());
                if (user != null && user.canFly()) {
                    user.enableFly();
                    return true;
                }
            } catch (Throwable ex) {
                McwwsAxiomSurvivalPlugin.getInstance().getLogger().fine(
                        "FlyWithFood enableFly 失败: " + ex.getMessage()
                );
            }
        }
        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
        if (!player.isFlying()) {
            player.setFlying(true);
        }
        return true;
    }

    static void disableFly(Player player) {
        if (player == null) {
            return;
        }
        if (available()) {
            try {
                FWFUser user = FWFUser.of(player.getName());
                if (user != null) {
                    user.disableFly();
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
        player.setFlying(false);
        player.setAllowFlight(false);
    }
}
