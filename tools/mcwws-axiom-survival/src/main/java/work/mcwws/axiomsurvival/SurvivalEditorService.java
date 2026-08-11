package work.mcwws.axiomsurvival;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配合客户端模组 {@code MCWWS_AxiomSurvivalClient}：玩家服务端始终保持生存，
 * 由 FlyWithFood 提供飞行；进入/退出 Editor 时由客户端通道通知。
 */
final class SurvivalEditorService {

    private final McwwsAxiomSurvivalPlugin plugin;
    private final EditorRestoreService editorRestoreService;
    private final Map<UUID, Long> clientEditorSessions = new ConcurrentHashMap<>();
    private final Map<UUID, MenuSnapshot> menuSnapshots = new ConcurrentHashMap<>();

    /** 开菜单前的建造位置与飞行状态；飞行权限由服务端自己记录，客户端本地创造会污染 abilities */
    private record MenuSnapshot(Location location, boolean allowFlight, boolean flying) {
    }

    SurvivalEditorService(McwwsAxiomSurvivalPlugin plugin, EditorRestoreService editorRestoreService) {
        this.plugin = plugin;
        this.editorRestoreService = editorRestoreService;
    }

    boolean enabled() {
        return plugin.getPluginConfig().getBoolean("survival-editor-mode", true);
    }

    boolean isClientEditorSession(Player player) {
        return player != null && clientEditorSessions.containsKey(player.getUniqueId());
    }

    void onClientEditorEnter(Player player) {
        if (!enabled() || player == null || !player.hasPermission("mcwws.axiom.survival.use")) {
            return;
        }
        if (!BlockProtection.isSurvivalLike(player) && player.getGameMode() != GameMode.SPECTATOR) {
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        editorRestoreService.onEnterSpectator(player);
        clientEditorSessions.put(player.getUniqueId(), System.currentTimeMillis());
        if (plugin.getPluginConfig().getBoolean("enable-flywithfood-on-editor", true)) {
            FlyWithFoodBridge.enableFly(player);
        }
        plugin.getLogger().info("生存 Editor 进入: " + player.getName());
    }

    /**
     * 客户端打开 Editor 菜单：记录建造位置供关菜单传送回来；客户端本地旁观会强制飞行，
     * 服务端同步置为飞行，否则服务端会在菜单期间累积摔落距离。
     */
    void onClientMenuOpen(Player player) {
        if (!enabled() || player == null) {
            return;
        }
        Location location = player.getLocation();
        if (location.getWorld() == null) {
            return;
        }
        menuSnapshots.put(player.getUniqueId(), new MenuSnapshot(
                location.clone(), player.getAllowFlight(), player.isFlying()
        ));
        plugin.getLogger().fine(String.format(
                "菜单打开快照: %s allowFlight=%s isFlying=%s",
                player.getName(), player.getAllowFlight(), player.isFlying()
        ));
        holdFlightDuringMenu(player);
    }

    /**
     * 客户端关闭 Editor 菜单：由服务端权威传送回开菜单前的位置，并按客户端快照下发飞行状态。
     * 客户端单方面改 {@code abilities.flying} 会被 {@code PlayerToggleFlightEvent} 的处理插件顶回去，
     * 必须由服务端 {@code setFlying} 下发权威 abilities 包，两端才一致（否则会出现在飞却吃摔落伤害）。
     */
    void onClientMenuClose(Player player, boolean flying) {
        if (player == null) {
            return;
        }
        MenuSnapshot snapshot = menuSnapshots.remove(player.getUniqueId());
        plugin.getLogger().fine(String.format(
                "菜单关闭: %s 客户端上报 flying=%s，快照 allowFlight=%s flying=%s",
                player.getName(), flying,
                snapshot == null ? "无" : snapshot.allowFlight(),
                snapshot == null ? "无" : snapshot.flying()
        ));
        if (snapshot == null || !player.isOnline() || snapshot.location().getWorld() == null) {
            return;
        }
        player.teleport(snapshot.location());
        player.setFallDistance(0f);
        restoreFlight(player, snapshot);
        // FlyWithFood 等插件可能在同 tick 内改回飞行状态，下一 tick 再重申一次
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                restoreFlight(player, snapshot);
                player.setFallDistance(0f);
                plugin.getLogger().fine(String.format(
                        "菜单关闭后一 tick: %s allowFlight=%s isFlying=%s",
                        player.getName(), player.getAllowFlight(), player.isFlying()
                ));
            }
        }, 1L);
    }

    /** 菜单期间客户端本地旁观强制飞行，服务端同步跟上，否则会凭空累积摔落距离 */
    private void holdFlightDuringMenu(Player player) {
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    /**
     * 先同步 FlyWithFood 内部状态，再无条件调用 setAllowFlight/setFlying，
     * 确保权威 abilities 包一定重新下发，客户端只能跟随。
     */
    private void restoreFlight(Player player, MenuSnapshot snapshot) {
        FlyWithFoodBridge.restoreFlyState(player, snapshot.allowFlight(), snapshot.flying());
        player.setAllowFlight(snapshot.allowFlight());
        player.setFlying(snapshot.allowFlight() && snapshot.flying());
    }

    void onClientEditorExit(Player player) {
        if (player == null) {
            return;
        }
        menuSnapshots.remove(player.getUniqueId());
        Long removed = clientEditorSessions.remove(player.getUniqueId());
        if (removed == null && !EditorSessionState.has(player)) {
            return;
        }
        boolean hadSnapshot = EditorSessionState.has(player);
        editorRestoreService.restoreNow(player);
        if (!hadSnapshot && plugin.getPluginConfig().getBoolean("disable-fly-on-editor-exit", true)) {
            FlyWithFoodBridge.disableFly(player);
        }
        plugin.getLogger().info("生存 Editor 退出: " + player.getName());
    }

    void clear(Player player) {
        if (player != null) {
            clientEditorSessions.remove(player.getUniqueId());
            menuSnapshots.remove(player.getUniqueId());
        }
    }
}
