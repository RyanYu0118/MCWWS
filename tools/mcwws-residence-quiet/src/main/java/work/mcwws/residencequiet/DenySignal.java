package work.mcwws.residencequiet;

import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记下 Residence 刚刚对谁发过拒绝提示。
 *
 * <p>Residence 的交互监听是 LOWEST 且只发消息不取消动作，本插件在 MONITOR 收尾时
 * 靠这个标记判断「这一次交互刚被判定为没权限」，从而真正取消它。
 */
final class DenySignal {

    private final Map<UUID, Integer> lastDenyTick = new ConcurrentHashMap<>();

    void mark(UUID playerId) {
        if (playerId != null) {
            lastDenyTick.put(playerId, Bukkit.getCurrentTick());
        }
    }

    boolean seenThisTick(UUID playerId) {
        Integer tick = lastDenyTick.get(playerId);
        return tick != null && tick == Bukkit.getCurrentTick();
    }

    void forget(UUID playerId) {
        lastDenyTick.remove(playerId);
    }

    void clearAll() {
        lastDenyTick.clear();
    }
}
