package work.mcwws.axiomsurvival;

import com.moulberry.axiom.event.AxiomGameModeChangeEvent;
import com.moulberry.axiom.event.AxiomHandshakeEvent;
import com.moulberry.axiom.event.AxiomTeleportEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public final class AxiomSurvivalListener implements Listener {

    private final McwwsAxiomSurvivalPlugin plugin;
    private final EditorRestoreService editorRestoreService;
    private final SurvivalEditorService survivalEditorService;

    public AxiomSurvivalListener(
            McwwsAxiomSurvivalPlugin plugin,
            ChargeService ignoredCharge,
            EditorRestoreService editorRestoreService,
            SurvivalEditorService survivalEditorService
    ) {
        this.plugin = plugin;
        this.editorRestoreService = editorRestoreService;
        this.survivalEditorService = survivalEditorService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHandshake(AxiomHandshakeEvent event) {
        if (!plugin.getPluginConfig().getBoolean("enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (!player.hasPermission("mcwws.axiom.survival.use")) {
            event.setCancelled(true);
            McwwsAxiomSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                    "handshake-denied",
                    "reason", "缺少 mcwws.axiom.survival.use 权限"
            ));
            return;
        }
        if (!BlockProtection.isSurvivalLike(player)) {
            event.setCancelled(true);
            McwwsAxiomSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                    "handshake-denied",
                    "reason", stripColor(plugin.msg("not-survival"))
            ));
            return;
        }
        double minBalance = plugin.getPluginConfig().getDouble("min-balance-to-enable", 0D);
        if (minBalance > 0D && EconomyService.getBalance(player) + 1e-6 < minBalance) {
            event.setCancelled(true);
            McwwsAxiomSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                    "handshake-denied",
                    "reason", "余额低于 " + EconomyService.format(minBalance)
            ));
            return;
        }
        McwwsAxiomSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("axiom-ready"));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> SurvivalEditorChannel.sendHello(player), 5L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAxiomGameMode(AxiomGameModeChangeEvent event) {
        if (!plugin.getPluginConfig().getBoolean("enabled", true) || !editorRestoreService.enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || BlockProtection.shouldBypass(player) || !player.hasPermission("mcwws.axiom.survival.use")) {
            return;
        }
        // 菜单打开到「进入 Editor」上报之间还没有会话，这段时间的模式同样由服务端掌控
        if (survivalEditorService.isClientEditorSession(player) || survivalEditorService.isMenuOpen(player)) {
            event.setCancelled(true);
            return;
        }
        GameMode requested = event.getGameMode();
        if (requested == GameMode.SPECTATOR) {
            editorRestoreService.onEnterSpectator(player);
            return;
        }
        if (EditorSessionState.isInRestoreGrace(player)) {
            event.setCancelled(true);
            return;
        }
        if (EditorSessionState.has(player)) {
            event.setCancelled(true);
            editorRestoreService.restoreNow(player);
            return;
        }
        if (requested == GameMode.CREATIVE
                && plugin.getPluginConfig().getBoolean("block-axiom-creative-switch", true)) {
            event.setCancelled(true);
            McwwsAxiomSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("creative-blocked"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAxiomTeleport(AxiomTeleportEvent event) {
        if (!editorRestoreService.enabled()) {
            return;
        }
        if (EditorSessionState.isInRestoreGrace(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBukkitGamemode(PlayerGameModeChangeEvent event) {
        if (!plugin.getPluginConfig().getBoolean("enabled", true) || !editorRestoreService.enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || !player.hasPermission("mcwws.axiom.survival.use") || BlockProtection.shouldBypass(player)) {
            return;
        }
        if (survivalEditorService.isClientEditorSession(player)) {
            GameMode next = event.getNewGameMode();
            // 菜单期间的旁观是服务端自己发起的，不能拦
            if (next == GameMode.SPECTATOR && survivalEditorService.isMenuOpen(player)) {
                return;
            }
            if (next != GameMode.SURVIVAL && next != GameMode.ADVENTURE) {
                event.setCancelled(true);
            }
            return;
        }
        GameMode to = event.getNewGameMode();
        if (to == GameMode.SPECTATOR && BlockProtection.isSurvivalLike(player)) {
            editorRestoreService.onEnterSpectator(player);
            return;
        }
        // 菜单期间的模式切换是服务端自己发起的，别当成退出 Editor
        if (EditorSessionState.has(player) && to != GameMode.SPECTATOR
                && !survivalEditorService.isMenuOpen(player)) {
            editorRestoreService.scheduleRestore(player, 1L);
        }
    }

    private static String stripColor(String input) {
        return input == null ? "" : input.replaceAll("§.", "");
    }
}
