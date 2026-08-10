package work.mcwws.axiomsurvival;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

final class EditorRestoreService {

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

    void onExitAttempt(Player player, GameMode requested) {
        if (!enabled() || player == null || !EditorSessionState.has(player)) {
            return;
        }
        if (requested == GameMode.CREATIVE) {
            scheduleRestore(player, 1L);
            return;
        }
        if (requested == GameMode.SURVIVAL || requested == GameMode.ADVENTURE) {
            scheduleRestore(player, 2L);
        }
    }

    void onAxiomTeleport(Player player) {
        if (!enabled() || player == null || !EditorSessionState.has(player)) {
            return;
        }
        if (player.getGameMode() != GameMode.SPECTATOR) {
            return;
        }
        scheduleRestore(player, 2L);
    }

    void onGamemodeChanged(Player player, GameMode newMode) {
        if (!enabled() || player == null || !EditorSessionState.has(player)) {
            return;
        }
        if (newMode == GameMode.SURVIVAL || newMode == GameMode.ADVENTURE) {
            scheduleRestore(player, 1L);
        }
    }

    void restoreNow(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        EditorSessionState.restoreAndClear(player);
        McwwsAxiomSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("editor-restored"));
    }

    void scheduleRestore(Player player, long delayTicks) {
        if (player == null) {
            return;
        }
        UUIDHolder id = new UUIDHolder(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player online = Bukkit.getPlayer(id.uuid());
            if (online == null || !online.isOnline() || !EditorSessionState.has(online)) {
                return;
            }
            EditorSessionState.restoreAndClear(online);
        }, delayTicks);
    }

    private record UUIDHolder(java.util.UUID uuid) {
    }
}
