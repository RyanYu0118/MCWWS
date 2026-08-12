package work.mcwws.axiomsurvival;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
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
    /** 菜单开着掉线的玩家：重新上线时要补一次模式与位置修正 */
    private final Map<UUID, MenuSnapshot> pendingRejoinFixes = new ConcurrentHashMap<>();
    private final Set<UUID> announced = ConcurrentHashMap.newKeySet();

    /** 开菜单前的建造位置、飞行状态与游戏模式；飞行权限由服务端自己记录，客户端本地创造会污染 abilities */
    private record MenuSnapshot(Location location, boolean allowFlight, boolean flying, GameMode mode) {
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

    /** 菜单期间服务端会主动切旁观，模式变更监听要放行这一次 */
    boolean isMenuOpen(Player player) {
        return player != null && menuSnapshots.containsKey(player.getUniqueId());
    }

    private boolean spectatorDuringMenu() {
        return plugin.getPluginConfig().getBoolean("spectator-during-menu", true);
    }

    /** 收到客户端模组的首条消息即证明生存 Editor 可用，本次登录只提示一次 */
    void noteClientPresent(Player player) {
        if (!enabled() || player == null || !announced.add(player.getUniqueId())) {
            return;
        }
        McwwsAxiomSurvivalPlugin.sendMessage(player,
                plugin.msg("prefix") + plugin.msg("survival-editor-ready"));
    }

    void onClientEditorEnter(Player player) {
        if (!enabled() || player == null || !player.hasPermission("mcwws.axiom.survival.use")) {
            return;
        }
        if (!BlockProtection.isSurvivalLike(player) && player.getGameMode() != GameMode.SPECTATOR) {
            return;
        }
        // 客户端是「先开菜单、后报进入 Editor」，此时的旁观是菜单期间由服务端自己切的，
        // 再强制切回生存会被模式监听器当成退出 Editor 而触发恢复
        if (player.getGameMode() == GameMode.SPECTATOR && !isMenuOpen(player)) {
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
     * 客户端打开 Editor 菜单：记录建造位置供关菜单传送回来，并把服务端也切成旁观，
     * 与客户端本地状态一致，菜单期间不会受伤、不会累积摔落距离。
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
                location.clone(), player.getAllowFlight(), player.isFlying(), player.getGameMode()
        ));
        plugin.getLogger().fine(String.format(
                "菜单打开快照: %s allowFlight=%s isFlying=%s mode=%s",
                player.getName(), player.getAllowFlight(), player.isFlying(), player.getGameMode()
        ));
        if (spectatorDuringMenu()) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SPECTATOR);
            }
            return;
        }
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
        // 先切回模式再下发飞行：setGameMode 会重置 abilities
        restoreGameMode(player, snapshot);
        player.setFallDistance(0f);
        restoreFlight(player, snapshot);
        // FlyWithFood 等插件可能在同 tick 内改回飞行状态，下一 tick 再重申一次
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                restoreGameMode(player, snapshot);
                restoreFlight(player, snapshot);
                player.setFallDistance(0f);
                plugin.getLogger().fine(String.format(
                        "菜单关闭后一 tick: %s allowFlight=%s isFlying=%s",
                        player.getName(), player.getAllowFlight(), player.isFlying()
                ));
            }
        }, 1L);
    }

    /** 菜单期间切了旁观就要切回来；快照本身是旁观（异常情况）时退回生存，与其余恢复路径一致 */
    private void restoreGameMode(Player player, MenuSnapshot snapshot) {
        GameMode target = snapshot.mode();
        if (target == null || target == GameMode.SPECTATOR) {
            target = GameMode.SURVIVAL;
        }
        if (player.getGameMode() != target) {
            player.setGameMode(target);
        }
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
        if (player == null) {
            return;
        }
        clientEditorSessions.remove(player.getUniqueId());
        MenuSnapshot snapshot = menuSnapshots.remove(player.getUniqueId());
        announced.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        // 菜单开着掉线：旁观状态与相机位置都会被写进存档，下次上线既卡旁观又可能卡在方块里。
        // 退出事件里的传送不一定生效，所以同时留一份待办，重新上线时再兜一次。
        pendingRejoinFixes.put(player.getUniqueId(), snapshot);
        applyMenuSnapshot(player, snapshot);
    }

    /** 重新上线时兜底修正上次掉线遗留的旁观模式与相机位置 */
    void onJoin(Player player) {
        if (player == null) {
            return;
        }
        MenuSnapshot snapshot = pendingRejoinFixes.remove(player.getUniqueId());
        if (snapshot != null) {
            applyMenuSnapshot(player, snapshot);
            plugin.getLogger().info("修正掉线遗留的菜单状态: " + player.getName());
        }
    }

    private void applyMenuSnapshot(Player player, MenuSnapshot snapshot) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            restoreGameMode(player, snapshot);
        }
        if (snapshot.location().getWorld() == null) {
            return;
        }
        try {
            player.teleport(snapshot.location());
            player.setFallDistance(0f);
        } catch (RuntimeException ex) {
            plugin.getLogger().fine("回位失败（多半是连接已断）: " + ex.getMessage());
        }
    }
}
