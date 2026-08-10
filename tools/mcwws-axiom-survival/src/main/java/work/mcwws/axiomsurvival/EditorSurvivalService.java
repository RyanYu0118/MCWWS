package work.mcwws.axiomsurvival;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

final class EditorSurvivalService {

    private final McwwsAxiomSurvivalPlugin plugin;

    EditorSurvivalService(McwwsAxiomSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    boolean enabled() {
        return plugin.getPluginConfig().getBoolean("keep-survival-gamemode", true);
    }

    boolean shouldEnableFlyOnEditor() {
        return plugin.getPluginConfig().getBoolean("enable-flywithfood-on-editor", true);
    }

    void onEditorEnter(Player player) {
        if (!enabled() || player == null || !player.hasPermission("mcwws.axiom.survival.use")) {
            return;
        }
        if (!BlockProtection.isSurvivalLike(player) && player.getGameMode() != GameMode.SPECTATOR) {
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        if (shouldEnableFlyOnEditor()) {
            FlyWithFoodBridge.enableFly(player);
        }
        plugin.getLogger().info("Editor 生存模式: " + player.getName() + "（已阻止旁观，启用飞行）");
    }

    void forceSurvival(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        GameMode mode = player.getGameMode();
        if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        FlyWithFoodBridge.disableFly(player);
        McwwsAxiomSurvivalPlugin.sendMessage(
                player,
                plugin.msg("prefix") + plugin.msg("editor-survival-reset")
        );
        plugin.getLogger().info("Editor 强制生存: " + player.getName());
    }
}
