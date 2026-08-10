package work.mcwws.axiomsurvival;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Editor 内相机由 Axiom teleport 驱动；退出后客户端恢复 vanilla 移动包。
 * 进入 Editor 后有宽限期，避免 Shift 进入时误触发恢复。
 * 客户端生存 Editor 会话由 {@link SurvivalEditorChannel} 处理，不走此路径。
 */
final class EditorVanillaMoveListener implements Listener {

    private final EditorRestoreService editorRestoreService;
    private final SurvivalEditorService survivalEditorService;

    EditorVanillaMoveListener(
            EditorRestoreService editorRestoreService,
            SurvivalEditorService survivalEditorService
    ) {
        this.editorRestoreService = editorRestoreService;
        this.survivalEditorService = survivalEditorService;
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
        if (survivalEditorService.isClientEditorSession(player)) {
            return;
        }
        if (EditorSessionState.isInRestoreGrace(player)) {
            return;
        }
        if (!EditorSessionState.shouldRestoreOnVanillaMove(player, editorRestoreService.enterGraceTicks())) {
            return;
        }
        McwwsAxiomSurvivalPlugin.getInstance().getLogger().info(
                "Editor 退出检测（vanilla 移动）: " + player.getName()
        );
        editorRestoreService.restoreNow(player);
    }
}
