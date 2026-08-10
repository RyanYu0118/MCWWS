package work.mcwws.axiomsurvival;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

final class EditorRestoreService {

    private static final long RESTORE_GRACE_MS = 3000L;

    private final McwwsAxiomSurvivalPlugin plugin;

    EditorRestoreService(McwwsAxiomSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    boolean enabled() {
        return plugin.getPluginConfig().getBoolean("restore-editor-on-exit", true);
    }

    void onEnterSpectator(Player player) {
        if (!enabled() || player == null || !player.hasPermission("mcwws.axiom.survival.use")) {
            return;
        }
        EditorSessionState.capture(player);
    }

    void restoreNow(Player player) {
        if (!enabled() || player == null || !player.isOnline()) {
            return;
        }
        boolean hadSnapshot = EditorSessionState.has(player);
        if (hadSnapshot) {
            EditorSessionState.restoreAndClear(player);
            EditorSessionState.beginRestoreGrace(player, RESTORE_GRACE_MS);
            McwwsAxiomSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("editor-restored"));
            plugin.getLogger().info("Editor 恢复: " + player.getName());
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
            player.setFlying(false);
            player.setAllowFlight(false);
            McwwsAxiomSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("editor-restored"));
            plugin.getLogger().info("Editor 强制生存: " + player.getName());
        }
    }

    void scheduleRestore(Player player, long delayTicks) {
        if (player == null) {
            return;
        }
        java.util.UUID id = player.getUniqueId();
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(id);
            if (online != null && online.isOnline()) {
                restoreNow(online);
            }
        }, delayTicks);
    }
}
