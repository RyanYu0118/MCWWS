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
 * </ul>
 */
public final class SurvivalEditorChannel implements PluginMessageListener {

    static final String CHANNEL = "mcwws:axiom_survival";
    private static final byte OP_ENTER = 1;
    private static final byte OP_EXIT = 0;

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
        byte op = message[0];
        if (op == OP_ENTER) {
            survivalEditorService.onClientEditorEnter(player);
            return;
        }
        if (op == OP_EXIT) {
            survivalEditorService.onClientEditorExit(player);
        }
    }
}
