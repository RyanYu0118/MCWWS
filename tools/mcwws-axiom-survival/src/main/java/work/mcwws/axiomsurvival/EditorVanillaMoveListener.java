package work.mcwws.axiomsurvival;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Editor 内相机由 Axiom {@code teleport} 包驱动，不产生 vanilla 移动包。
 * Right Shift 关闭 Editor 后客户端恢复发送移动/视角包，据此立即恢复生存与位置。
 */
final class EditorVanillaMoveListener implements Listener {

    private final EditorRestoreService editorRestoreService;

    EditorVanillaMoveListener(EditorRestoreService editorRestoreService) {
        this.editorRestoreService = editorRestoreService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!editorRestoreService.enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null
                || !player.hasPermission("mcwws.axiom.survival.use")
                || BlockProtection.shouldBypass(player)) {
            return;
        }
        if (player.getGameMode() != GameMode.SPECTATOR || !EditorSessionState.has(player)) {
            return;
        }
        if (EditorSessionState.isInRestoreGrace(player)) {
            return;
        }
        if (!EditorSessionState.shouldRestoreOnVanillaMove(player)) {
            return;
        }
        McwwsAxiomSurvivalPlugin.getInstance().getLogger().info(
                "Editor 退出检测（vanilla 移动）: " + player.getName()
        );
        editorRestoreService.restoreNow(player);
    }
}
