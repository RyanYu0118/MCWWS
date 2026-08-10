package work.mcwws.axiomsurvival;

import com.moulberry.axiom.event.AxiomGameModeChangeEvent;
import com.moulberry.axiom.event.AxiomHandshakeEvent;
import com.moulberry.axiom.event.AxiomTeleportEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class AxiomSurvivalListener implements Listener {

    private final McwwsAxiomSurvivalPlugin plugin;
    private final EditorRestoreService editorRestoreService;

    public AxiomSurvivalListener(
            McwwsAxiomSurvivalPlugin plugin,
            ChargeService ignoredCharge,
            EditorRestoreService editorRestoreService
    ) {
        this.plugin = plugin;
        this.editorRestoreService = editorRestoreService;
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

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        EditorSessionState.clear(event.getPlayer());
    }

    private static String stripColor(String input) {
        return input == null ? "" : input.replaceAll("§.", "");
    }
}
