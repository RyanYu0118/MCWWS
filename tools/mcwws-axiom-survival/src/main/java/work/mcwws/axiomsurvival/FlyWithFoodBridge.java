package work.mcwws.axiomsurvival;

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
                Class<?> fwfUserClass = Class.forName("me.xpyex.plugin.flywithfood.common.implementation.FWFUser");
                Object user = fwfUserClass.getMethod("of", String.class).invoke(null, player.getName());
                if (user != null && (boolean) fwfUserClass.getMethod("canFly").invoke(user)) {
                    fwfUserClass.getMethod("enableFly").invoke(user);
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
                Class<?> fwfUserClass = Class.forName("me.xpyex.plugin.flywithfood.common.implementation.FWFUser");
                Object user = fwfUserClass.getMethod("of", String.class).invoke(null, player.getName());
                if (user != null) {
                    fwfUserClass.getMethod("disableFly").invoke(user);
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
        player.setFlying(false);
        player.setAllowFlight(false);
    }
}
