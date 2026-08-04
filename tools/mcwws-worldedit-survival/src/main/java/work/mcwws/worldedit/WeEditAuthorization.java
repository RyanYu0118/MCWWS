package work.mcwws.worldedit;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生存扣费与 FAWE 异步改块之间的闸门：仅在有「已扣费」或「撤销/重做」授权时允许 EditSession 写方块。
 * 防止 CommandEvent 已取消（预估 0 格）但 FAWE 仍执行 replace 的白嫖。
 */
final class WeEditAuthorization {

    private enum Kind {
        PAID,
        HISTORY
    }

    private record Pass(Kind kind, long remaining, long expiresAtMs) {
        boolean expired() {
            return System.currentTimeMillis() > expiresAtMs;
        }
    }

    private static final Map<UUID, Pass> active = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> deniedUntil = new ConcurrentHashMap<>();
    private static final long PAID_TTL_MS = 120_000L;
    private static final long HISTORY_TTL_MS = 60_000L;
    private static final long DENY_MS = 8_000L;

    private WeEditAuthorization() {
    }

    static void grantPaid(Player player, long blockBudget) {
        if (player == null || blockBudget <= 0L) {
            return;
        }
        deniedUntil.remove(player.getUniqueId());
        active.put(player.getUniqueId(), new Pass(Kind.PAID, blockBudget, System.currentTimeMillis() + PAID_TTL_MS));
    }

    static void grantHistory(Player player) {
        if (player == null) {
            return;
        }
        deniedUntil.remove(player.getUniqueId());
        active.put(player.getUniqueId(), new Pass(Kind.HISTORY, Long.MAX_VALUE, System.currentTimeMillis() + HISTORY_TTL_MS));
    }

    static void clear(Player player) {
        if (player != null) {
            active.remove(player.getUniqueId());
        }
    }

    /** 命令因预估失败/余额不足等被取消：撤销写块授权并短暂禁止未扣费写入。 */
    static void revokeUnpaid(Player player) {
        if (player == null) {
            return;
        }
        active.remove(player.getUniqueId());
        deniedUntil.put(player.getUniqueId(), System.currentTimeMillis() + DENY_MS);
    }

    static boolean tryConsumeBlock(Player player) {
        if (player == null) {
            return false;
        }
        McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
        if (plugin != null && !plugin.getPluginConfig().getBoolean("edit-authorization.enabled", true)) {
            return true;
        }
        Long deny = deniedUntil.get(player.getUniqueId());
        if (deny != null) {
            if (System.currentTimeMillis() < deny) {
                return false;
            }
            deniedUntil.remove(player.getUniqueId());
        }
        Pass pass = active.get(player.getUniqueId());
        if (pass == null || pass.expired()) {
            active.remove(player.getUniqueId());
            return false;
        }
        if (pass.kind() == Kind.HISTORY) {
            return true;
        }
        if (pass.remaining() <= 0L) {
            active.remove(player.getUniqueId());
            return false;
        }
        long left = pass.remaining() - 1L;
        if (left <= 0L) {
            active.remove(player.getUniqueId());
        } else {
            active.put(player.getUniqueId(), new Pass(Kind.PAID, left, pass.expiresAtMs()));
        }
        return true;
    }
}
