package work.mcwws.axiomsurvival;

import com.moulberry.axiom.event.AxiomGameModeChangeEvent;
import com.moulberry.axiom.event.AxiomHandshakeEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class AxiomSurvivalListener implements Listener {

    private final McwwsAxiomSurvivalPlugin plugin;
    private final ChargeService chargeService;

    public AxiomSurvivalListener(McwwsAxiomSurvivalPlugin plugin, ChargeService chargeService, AxiomPaperHook ignored) {
        this.plugin = plugin;
        this.chargeService = chargeService;
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
        if (player == null || BlockProtection.shouldBypass(player)) {
            return;
        }
        if (!player.hasPermission("mcwws.axiom.survival.use")) {
            return;
        }

        boolean restoreOnExit = plugin.getPluginConfig().getBoolean("restore-editor-on-exit", true);

        if (restoreOnExit && event.getGameMode() == GameMode.SPECTATOR && BlockProtection.isSurvivalLike(player)) {
            EditorSessionState.capture(player);
            return;
        }

        if (event.getGameMode() == GameMode.CREATIVE) {
            if (!plugin.getPluginConfig().getBoolean("block-axiom-creative-switch", true)) {
                return;
            }
            event.setCancelled(true);
            if (restoreOnExit && EditorSessionState.has(player)) {
                scheduleEditorRestore(player);
                return;
            }
            McwwsAxiomSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("creative-blocked"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAxiomGameModeMonitor(AxiomGameModeChangeEvent event) {
        if (!plugin.getPluginConfig().getBoolean("restore-editor-on-exit", true)) {
            return;
        }
        if (!isSurvivalLike(event.getGameMode())) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || !EditorSessionState.has(player)) {
            return;
        }
        scheduleEditorRestore(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        EditorSessionState.clear(event.getPlayer());
    }

    private void scheduleEditorRestore(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> EditorSessionState.restoreAndClear(player));
    }

    private static boolean isSurvivalLike(GameMode mode) {
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    private static String stripColor(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("§.", "");
    }
}
