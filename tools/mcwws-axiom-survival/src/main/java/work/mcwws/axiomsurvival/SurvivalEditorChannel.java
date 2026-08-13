package work.mcwws.axiomsurvival;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端模组通道 {@code mcwws:axiom_survival}：
 * <ul>
 *   <li>服务端 → 客户端 {@code hello}：宣告支持生存 Editor；附 {@code |gizmo} 时允许实体操纵小方块</li>
 *   <li>客户端 → 服务端 {@code 1}：进入 Editor</li>
 *   <li>客户端 → 服务端 {@code 0}：退出 Editor</li>
 *   <li>客户端 → 服务端 {@code 2}：打开 Editor 菜单（快照位置）</li>
 *   <li>客户端 → 服务端 {@code 3}：关闭 Editor 菜单（传送回快照，第二字节为开菜单前的飞行状态）</li>
 * </ul>
 */
public final class SurvivalEditorChannel implements PluginMessageListener {

    static final String CHANNEL = "mcwws:axiom_survival";
    private static final byte OP_ENTER = 1;
    private static final byte OP_EXIT = 0;
    private static final byte OP_MENU_OPEN = 2;
    private static final byte OP_MENU_CLOSE = 3;
    private static final Map<UUID, Boolean> lastGizmoFlag = new ConcurrentHashMap<>();

    private final SurvivalEditorService survivalEditorService;

    SurvivalEditorChannel(SurvivalEditorService survivalEditorService) {
        this.survivalEditorService = survivalEditorService;
    }

    static void sendHello(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        McwwsAxiomSurvivalPlugin plugin = McwwsAxiomSurvivalPlugin.getInstance();
        if (!plugin.getPluginConfig().getBoolean("survival-editor-mode", true)) {
            return;
        }
        boolean gizmo = allowsEntityGizmo(player);
        lastGizmoFlag.put(player.getUniqueId(), gizmo);
        String text = gizmo ? "hello|gizmo" : "hello";
        player.sendPluginMessage(plugin, CHANNEL, text.getBytes(StandardCharsets.UTF_8));
    }

    static void syncHelloIfNeeded(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        boolean gizmo = allowsEntityGizmo(player);
        Boolean last = lastGizmoFlag.get(player.getUniqueId());
        if (last != null && last == gizmo) {
            return;
        }
        sendHello(player);
    }

    static void clear(Player player) {
        if (player != null) {
            lastGizmoFlag.remove(player.getUniqueId());
        }
    }

    static boolean allowsEntityGizmo(Player player) {
        if (player == null) {
            return false;
        }
        McwwsAxiomSurvivalPlugin plugin = McwwsAxiomSurvivalPlugin.getInstance();
        String perm = plugin.getPluginConfig().getString(
                "entity.gizmo-permission", "mcwws.axiom.survival.entity");
        return player.hasPermission(perm);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel) || player == null || message == null || message.length == 0) {
            return;
        }
        if (!survivalEditorService.enabled()) {
            return;
        }
        if (!player.hasPermission("mcwws.axiom.survival.use")) {
            return;
        }
        survivalEditorService.noteClientPresent(player);
        byte op = message[0];
        boolean flag = message.length > 1 && message[1] != 0;
        switch (op) {
            case OP_ENTER -> survivalEditorService.onClientEditorEnter(player);
            case OP_EXIT -> survivalEditorService.onClientEditorExit(player);
            case OP_MENU_OPEN -> survivalEditorService.onClientMenuOpen(player);
            case OP_MENU_CLOSE -> survivalEditorService.onClientMenuClose(player, flag);
            default -> { }
        }
    }
}
