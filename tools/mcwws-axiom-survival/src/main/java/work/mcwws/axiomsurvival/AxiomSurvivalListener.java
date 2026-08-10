package work.mcwws.axiomsurvival;

import com.moulberry.axiom.event.AxiomGameModeChangeEvent;
import com.moulberry.axiom.event.AxiomHandshakeEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class AxiomSurvivalListener implements Listener {

    private final McwwsAxiomSurvivalPlugin plugin;
    private final EditorSurvivalService editorSurvivalService;

    public AxiomSurvivalListener(
            McwwsAxiomSurvivalPlugin plugin,
            ChargeService ignoredCharge,
            EditorSurvivalService editorSurvivalService
    ) {
        this.plugin = plugin;
        this.editorSurvivalService = editorSurvivalService;
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
        if (!plugin.getPluginConfig().getBoolean("enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || BlockProtection.shouldBypass(player) || !player.hasPermission("mcwws.axiom.survival.use")) {
            return;
        }
        GameMode requested = event.getGameMode();
        if (requested == GameMode.SPECTATOR && editorSurvivalService.enabled()) {
            event.setCancelled(true);
            editorSurvivalService.onEditorEnter(player);
            return;
        }
        if (requested == GameMode.CREATIVE
                && plugin.getPluginConfig().getBoolean("block-axiom-creative-switch", true)) {
            event.setCancelled(true);
            McwwsAxiomSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("creative-blocked"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBukkitGamemode(PlayerGameModeChangeEvent event) {
        if (!plugin.getPluginConfig().getBoolean("enabled", true) || !editorSurvivalService.enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || !player.hasPermission("mcwws.axiom.survival.use") || BlockProtection.shouldBypass(player)) {
            return;
        }
        if (event.getNewGameMode() == GameMode.SPECTATOR && BlockProtection.isSurvivalLike(player)) {
            event.setCancelled(true);
            editorSurvivalService.onEditorEnter(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // no session state to clear
    }

    private static String stripColor(String input) {
        return input == null ? "" : input.replaceAll("§.", "");
    }
}
