package work.mcwws.axiomsurvival;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        // 掉线遗留的旁观/位置要先修，哪怕生存 Editor 已被关掉
        survivalEditorService.onJoin(event.getPlayer());
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
        plugin.getChargeNotifier().clear(event.getPlayer());
        plugin.getChargeHistory().clear(event.getPlayer());
        plugin.getUsageLimits().clear(event.getPlayer());
        SurvivalEditorChannel.clear(event.getPlayer());
    }
}
