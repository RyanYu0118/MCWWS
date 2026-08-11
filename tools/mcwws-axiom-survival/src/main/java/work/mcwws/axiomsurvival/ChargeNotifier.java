package work.mcwws.axiomsurvival;

import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扣费提示聚合。
 *
 * <p>Axiom 会把一次大范围编辑拆成许多 {@code set_buffer} 包，逐包提示会直接刷屏。
 * 这里把短时间内的扣费、退款、受保护方块累加起来，延迟若干 tick 只发一条汇总。
 * 拒绝类提示（余额不足、超限）按 key 限流，避免同一次编辑重复轰炸。
 */
final class ChargeNotifier {

    private static final class Pending {
        double demolition;
        double material;
        double labor;
        double total;
        long blocks;
        long protectedBlocks;
        double refundGross;
        double refundNet;
        double biomeFee;
        long biomeCells;
        final Map<String, long[]> entityCounts = new LinkedHashMap<>();
        final Map<String, double[]> entityFees = new LinkedHashMap<>();
        boolean flushScheduled;
    }

    private final McwwsAxiomSurvivalPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> lastDenyAt = new ConcurrentHashMap<>();

    ChargeNotifier(McwwsAxiomSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    void addBlockCharge(Player player, FeeAccumulator.Result estimate, double total) {
        Pending state = state(player);
        synchronized (state) {
            state.demolition += estimate.demolition();
            state.material += estimate.material();
            state.labor += estimate.labor();
            state.total += total;
            state.blocks += estimate.affectedBlocks();
        }
        schedule(player);
    }

    void addEntityCharge(Player player, String kind, long count, double fee) {
        Pending state = state(player);
        synchronized (state) {
            state.entityCounts.computeIfAbsent(kind, key -> new long[1])[0] += count;
            state.entityFees.computeIfAbsent(kind, key -> new double[1])[0] += fee;
        }
        schedule(player);
    }

    void addBiomeCharge(Player player, long cells, double fee) {
        Pending state = state(player);
        synchronized (state) {
            state.biomeCells += cells;
            state.biomeFee += fee;
        }
        schedule(player);
    }

    void addRefund(Player player, double gross, double net) {
        Pending state = state(player);
        synchronized (state) {
            state.refundGross += gross;
            state.refundNet += net;
        }
        schedule(player);
    }

    void addProtectedSkipped(Player player, long count) {
        Pending state = state(player);
        synchronized (state) {
            state.protectedBlocks += count;
        }
        schedule(player);
    }

    /** 拒绝类提示：同一 key 在限流窗口内只发一次 */
    void deny(Player player, String key, String message) {
        if (player == null) {
            return;
        }
        long throttle = Math.max(plugin.getPluginConfig().getLong("message-throttle-seconds", 3L), 0L) * 1000L;
        long now = System.currentTimeMillis();
        Map<String, Long> perKey = lastDenyAt.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>());
        Long last = perKey.get(key);
        if (last != null && now - last < throttle) {
            return;
        }
        perKey.put(key, now);
        McwwsAxiomSurvivalPlugin.sendMessage(player, message);
    }

    void clear(Player player) {
        if (player != null) {
            pending.remove(player.getUniqueId());
            lastDenyAt.remove(player.getUniqueId());
        }
    }

    private Pending state(Player player) {
        return pending.computeIfAbsent(player.getUniqueId(), id -> new Pending());
    }

    private void schedule(Player player) {
        Pending state = state(player);
        synchronized (state) {
            if (state.flushScheduled) {
                return;
            }
            state.flushScheduled = true;
        }
        long delay = Math.max(plugin.getPluginConfig().getLong("message-flush-ticks", 20L), 1L);
        UUID uuid = player.getUniqueId();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> flush(uuid), delay);
    }

    private void flush(UUID uuid) {
        Pending state = pending.remove(uuid);
        if (state == null) {
            return;
        }
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return;
        }
        String prefix = plugin.msg("prefix");
        synchronized (state) {
            if (state.blocks > 0L) {
                McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + plugin.msg(
                        "charged",
                        "total", EconomyService.format(FeeAccumulator.round(state.total)),
                        "blocks", String.valueOf(state.blocks),
                        "demolition", EconomyService.format(FeeAccumulator.round(state.demolition)),
                        "material", EconomyService.format(FeeAccumulator.round(state.material)),
                        "labor", EconomyService.format(FeeAccumulator.round(state.labor))
                ));
            }
            for (Map.Entry<String, long[]> entry : state.entityCounts.entrySet()) {
                double fee = state.entityFees.getOrDefault(entry.getKey(), new double[1])[0];
                McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + plugin.msg(
                        "charged-entity",
                        "kind", entry.getKey(),
                        "count", String.valueOf(entry.getValue()[0]),
                        "total", EconomyService.format(FeeAccumulator.round(fee))
                ));
            }
            if (state.biomeCells > 0L) {
                McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + plugin.msg(
                        "charged-biome",
                        "count", String.valueOf(state.biomeCells),
                        "total", EconomyService.format(FeeAccumulator.round(state.biomeFee))
                ));
            }
            if (state.refundNet > 0D) {
                double gross = FeeAccumulator.round(state.refundGross);
                double net = FeeAccumulator.round(state.refundNet);
                double fee = FeeAccumulator.round(gross - net);
                int percent = gross <= 0D ? 0 : (int) Math.round(fee / gross * 100D);
                McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + plugin.msg(
                        "undo-refunded",
                        "gross", EconomyService.format(gross),
                        "fee", EconomyService.format(fee),
                        "percent", String.valueOf(percent),
                        "net", EconomyService.format(net)
                ));
            }
            if (state.protectedBlocks > 0L) {
                McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + plugin.msg(
                        "protected-skipped",
                        "count", String.valueOf(state.protectedBlocks)
                ));
            }
        }
    }
}
