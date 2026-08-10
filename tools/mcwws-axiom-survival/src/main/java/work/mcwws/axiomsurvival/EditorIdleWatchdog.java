package work.mcwws.axiomsurvival;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

final class EditorIdleWatchdog implements Runnable {

    private final McwwsAxiomSurvivalPlugin plugin;
    private final EditorRestoreService editorRestoreService;

    EditorIdleWatchdog(McwwsAxiomSurvivalPlugin plugin, EditorRestoreService editorRestoreService) {
        this.plugin = plugin;
        this.editorRestoreService = editorRestoreService;
    }

    @Override
    public void run() {
        if (!editorRestoreService.enabled()) {
            return;
        }
        long idleMs = plugin.getPluginConfig().getLong("editor-idle-restore-ms", 1500L);
        if (idleMs <= 0L) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!EditorSessionState.has(player)) {
                continue;
            }
            if (player.getGameMode() != GameMode.SPECTATOR) {
                continue;
            }
            if (EditorSessionState.isInRestoreGrace(player)) {
                continue;
            }
            if (!EditorSessionState.shouldIdleRestore(player, idleMs)) {
                continue;
            }
            plugin.getLogger().info("Editor 空闲恢复: " + player.getName() + "（Right Shift 退出未切模式）");
            editorRestoreService.restoreNow(player);
        }
    }
}
