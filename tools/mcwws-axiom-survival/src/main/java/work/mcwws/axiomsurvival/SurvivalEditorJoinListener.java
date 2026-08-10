package work.mcwws.axiomsurvival;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

final class SurvivalEditorJoinListener implements Listener {

    private final McwwsAxiomSurvivalPlugin plugin;
    private final SurvivalEditorService survivalEditorService;

    SurvivalEditorJoinListener(McwwsAxiomSurvivalPlugin plugin, SurvivalEditorService survivalEditorService) {
        this.plugin = plugin;
        this.survivalEditorService = survivalEditorService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getPluginConfig().getBoolean("survival-editor-mode", true)) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> SurvivalEditorChannel.sendHello(event.getPlayer()),
                40L
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        survivalEditorService.clear(event.getPlayer());
        EditorSessionState.clear(event.getPlayer());
    }
}
