package work.mcwws.axiomsurvival;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

/**
 * 客户端模组通道 {@code mcwws:axiom_survival}：
 * <ul>
 *   <li>服务端 → 客户端 {@code hello}：宣告支持生存 Editor</li>
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
        byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
        player.sendPluginMessage(plugin, CHANNEL, payload);
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
