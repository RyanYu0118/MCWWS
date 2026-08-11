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
    private final Map<UUID, Location> menuSnapshots = new ConcurrentHashMap<>();

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

    /** 客户端打开 Editor 菜单：记录建造位置，供关菜单时传送回来 */
    void onClientMenuOpen(Player player) {
        if (!enabled() || player == null) {
            return;
        }
        Location location = player.getLocation();
        if (location.getWorld() == null) {
            return;
        }
        menuSnapshots.put(player.getUniqueId(), location.clone());
    }

    /** 客户端关闭 Editor 菜单：由服务端权威传送回开菜单前的位置 */
    void onClientMenuClose(Player player) {
        if (player == null) {
            return;
        }
        Location location = menuSnapshots.remove(player.getUniqueId());
        if (location == null || !player.isOnline() || location.getWorld() == null) {
            return;
        }
        player.teleport(location);
        player.setFallDistance(0f);
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
