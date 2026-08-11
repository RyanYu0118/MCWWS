package work.mcwws.axiomsurvival;

import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每位玩家最近若干笔已扣费的 Axiom 编辑。
 *
 * <p>Axiom 的撤销不是独立指令，而是把反向内容再发一个 {@code set_block}/{@code set_buffer} 包，
 * 服务端无法从协议上区分。这里按「放置/拆除计数互换且格数相同」判定反向编辑，
 * 命中即改为退款而不是二次扣费。大范围编辑会被拆成多包，因此保留一个队列逐包配对。
 */
final class ChargeHistory {

    record Entry(
            Map<String, Long> removedCounts,
            Map<String, Long> placedCounts,
            long affectedBlocks,
            double gross,
            String label,
            long timestamp,
            List<String> marketLines
    ) {
    }

    private final McwwsAxiomSurvivalPlugin plugin;
    private final Map<UUID, Deque<Entry>> history = new ConcurrentHashMap<>();

    ChargeHistory(McwwsAxiomSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    void record(Player player, FeeAccumulator.Result estimate, double gross, String label, List<String> marketLines) {
        if (player == null || estimate == null || gross <= 0D || !enabled()) {
            return;
        }
        Deque<Entry> queue = history.computeIfAbsent(player.getUniqueId(), key -> new ArrayDeque<>());
        synchronized (queue) {
            queue.addLast(new Entry(
                    estimate.removedCounts(),
                    estimate.placedCounts(),
                    estimate.affectedBlocks(),
                    gross,
                    label,
                    System.currentTimeMillis(),
                    marketLines == null ? List.of() : List.copyOf(marketLines)
            ));
            while (queue.size() > maxSize()) {
                queue.removeFirst();
            }
        }
    }

    /** 若本次编辑正好反转此前某笔已扣费编辑，取出并移除那笔记录 */
    Entry takeReverseMatch(Player player, FeeAccumulator.Result estimate) {
        if (player == null || estimate == null || estimate.affectedBlocks() <= 0L || !enabled()) {
            return null;
        }
        Deque<Entry> queue = history.get(player.getUniqueId());
        if (queue == null) {
            return null;
        }
        long expireBefore = System.currentTimeMillis() - windowMillis();
        synchronized (queue) {
            queue.removeIf(entry -> entry.timestamp() < expireBefore);
            for (var iterator = queue.descendingIterator(); iterator.hasNext(); ) {
                Entry entry = iterator.next();
                if (isReverseOf(entry, estimate)) {
                    iterator.remove();
                    return entry;
                }
            }
        }
        return null;
    }

    void clear(Player player) {
        if (player != null) {
            history.remove(player.getUniqueId());
        }
    }

    private static boolean isReverseOf(Entry entry, FeeAccumulator.Result estimate) {
        return entry.affectedBlocks() == estimate.affectedBlocks()
                && entry.placedCounts().equals(estimate.removedCounts())
                && entry.removedCounts().equals(estimate.placedCounts());
    }

    private boolean enabled() {
        return plugin.getPluginConfig().getBoolean("undo-refund.enabled", true);
    }

    private int maxSize() {
        return Math.max(plugin.getPluginConfig().getInt("undo-refund.history-size", 128), 1);
    }

    private long windowMillis() {
        long seconds = Math.max(plugin.getPluginConfig().getLong("undo-refund.match-window-seconds", 600L), 1L);
        return seconds * 1000L;
    }
}
