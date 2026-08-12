package work.mcwws.worldedit;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 上一笔生存创世神扣款（供 //undo 退款） */
final class WeChargeMemory {

    record LastCharge(double grossAmount, String command) {
    }

    private static final ConcurrentHashMap<UUID, LastCharge> LAST = new ConcurrentHashMap<>();

    private WeChargeMemory() {
    }

    /** grossAmount 为带符号净额：正数是实扣，负数是拆除回收后的净到账 */
    static void record(Player player, double grossAmount, String command) {
        if (player == null || Math.abs(grossAmount) < 0.01D) {
            return;
        }
        String cmd = command == null ? "worldedit" : command;
        LAST.put(player.getUniqueId(), new LastCharge(grossAmount, cmd));
    }

    static LastCharge take(Player player) {
        if (player == null) {
            return null;
        }
        return LAST.remove(player.getUniqueId());
    }
}
