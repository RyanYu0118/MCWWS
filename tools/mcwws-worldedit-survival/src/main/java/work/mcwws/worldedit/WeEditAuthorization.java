package work.mcwws.worldedit;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生存扣费与 FAWE 改块之间的闸门。
 * FAWE 的 PlatformCommandManager 根本不看 {@code CommandEvent.isCancelled()}，取消事件拦不住指令，
 * 因此「余额不足 / 缺领地权限 / 预估失败」必须靠这里的拒绝标记，在 EditSession 与写块两处兜住。
 * 没有结论的写入（笔刷、其他插件的编辑）一律放行，避免误伤正常功能。
 */
final class WeEditAuthorization {

    private enum State {
        ALLOWED,
        DENIED
    }

    private record Decision(State state, long expiresAtMs) {
        boolean expired() {
            return System.currentTimeMillis() > expiresAtMs;
        }
    }

    private static final Map<UUID, Decision> decisions = new ConcurrentHashMap<>();
    private static final long ALLOW_TTL_MS = 120_000L;
    private static final long DENY_TTL_MS = 8_000L;

    private WeEditAuthorization() {
    }

    /** 扣费成功、bypass 放行或撤销/重做：解除拒绝标记。 */
    static void allow(Player player) {
        if (player == null) {
            return;
        }
        decisions.put(player.getUniqueId(), new Decision(State.ALLOWED, System.currentTimeMillis() + ALLOW_TTL_MS));
    }

    /** 指令被判定不可执行：短时间内禁止该玩家的一切 WorldEdit 写块。 */
    static void deny(Player player) {
        if (player == null) {
            return;
        }
        decisions.put(player.getUniqueId(), new Decision(State.DENIED, System.currentTimeMillis() + DENY_TTL_MS));
    }

    static void reset(Player player) {
        if (player != null) {
            decisions.remove(player.getUniqueId());
        }
    }

    static boolean isDenied(Player player) {
        if (player == null || !enabled()) {
            return false;
        }
        Decision decision = decisions.get(player.getUniqueId());
        if (decision == null || decision.expired()) {
            decisions.remove(player.getUniqueId());
            return false;
        }
        return decision.state() == State.DENIED;
    }

    static boolean allowWrite(Player player) {
        return !isDenied(player);
    }

    private static boolean enabled() {
        McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
        return plugin == null || plugin.getPluginConfig().getBoolean("edit-authorization.enabled", true);
    }
}
