package work.mcwws.economyledger;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每笔余额变动的屏幕提示。
 *
 * <p>装了 {@code MCWWS_AxiomSurvivalClient} 的玩家看到左下角浮层，其余玩家回退到 action bar。
 * 同一分类、同一方向的连续变动在合并窗口内累加成一条（飞行每秒扣费、Axiom 逐包扣费都属于这种），
 * 单笔变动则原样单独显示，与网页零钱明细一致。
 */
final class BalanceNotifier {

    private record Pending(String category, double amount, long lastAt) {
    }

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");

    private final McwwsEconomyLedgerPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    BalanceNotifier(McwwsEconomyLedgerPlugin plugin) {
        this.plugin = plugin;
    }

    void notifyChange(Player player, double delta, double newBalance, String category, String description) {
        if (player == null || !player.isOnline() || !plugin.isNotifyEnabled()) {
            return;
        }
        if (Math.abs(delta) < 0.005D) {
            return;
        }
        String key = category == null || category.isBlank() ? "other" : category;
        long now = System.currentTimeMillis();
        Pending previous = pending.get(player.getUniqueId());
        boolean replace = previous != null
                && previous.category().equals(key)
                && sameDirection(previous.amount(), delta)
                && now - previous.lastAt() <= plugin.getNotifyMergeWindowMs();
        double shown = replace ? round(previous.amount() + delta) : round(delta);
        pending.put(player.getUniqueId(), new Pending(key, shown, now));

        String text = format(shown, newBalance, label(key, description));
        try {
            if (BalanceHudChannel.isSupported(player)) {
                BalanceHudChannel.send(plugin, player, replace, key, text);
            } else if (plugin.isNotifyActionBarFallback()) {
                player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
            }
        } catch (RuntimeException ex) {
            // 余额提示是附属功能，失败不能影响账本写入与经济流程
            plugin.getLogger().fine("推送余额提示失败: " + ex.getMessage());
        }
    }

    void clear(Player player) {
        if (player != null) {
            pending.remove(player.getUniqueId());
        }
    }

    private String format(double delta, double balance, String label) {
        String color = delta >= 0D ? plugin.getNotifyCreditColor() : plugin.getNotifyDebitColor();
        return plugin.getNotifyFormat()
                .replace("{color}", color)
                .replace("{sign}", delta >= 0D ? "+" : "-")
                .replace("{amount}", MONEY.format(Math.abs(delta)))
                .replace("{balance}", MONEY.format(balance))
                .replace("{label}", label)
                .replace('&', '§');
    }

    /**
     * 优先用配置里的分类中文名；没配就退回账本说明的前半段
     * （说明形如「Axiom 建造: set_buffer」，冒号后是技术细节，屏幕上不需要）。
     */
    private String label(String category, String description) {
        String configured = plugin.getNotifyLabel(category);
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        if (description == null || description.isBlank()) {
            return category;
        }
        int colon = description.indexOf(':');
        return colon > 0 ? description.substring(0, colon).trim() : description.trim();
    }

    private static boolean sameDirection(double a, double b) {
        return (a >= 0D) == (b >= 0D);
    }

    private static double round(double value) {
        return Math.round(value * 100D) / 100D;
    }
}
