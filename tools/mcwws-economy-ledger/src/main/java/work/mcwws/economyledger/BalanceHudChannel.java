package work.mcwws.economyledger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;

/**
 * 余额变动提示通道 {@code mcwws:balance_hud}（服务端 → 客户端）。
 *
 * <p>载荷是整包 UTF-8 文本 {@code <0|1>\0<key>\0<text>}：第一段为是否替换上一条同类提示，
 * 第二段是合并用的分组键，第三段是已经排版好的带 § 颜色文本。
 * 客户端模组 {@code MCWWS_AxiomSurvivalClient} 会把它画在屏幕左下角。
 */
final class BalanceHudChannel {

    static final String CHANNEL = "mcwws:balance_hud";

    private BalanceHudChannel() {
    }

    /** 客户端注册过该通道才说明装了模组，否则要走 action bar 兜底 */
    static boolean isSupported(Player player) {
        return player != null && player.getListeningPluginChannels().contains(CHANNEL);
    }

    static void send(Plugin plugin, Player player, boolean replace, String key, String text) {
        String payload = (replace ? "1" : "0") + "\u0000" + sanitize(key) + "\u0000" + text;
        player.sendPluginMessage(plugin, CHANNEL, payload.getBytes(StandardCharsets.UTF_8));
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace('\u0000', '_');
    }
}
