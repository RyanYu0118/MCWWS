package work.mcwws.worldedit;

import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 创世神方块变更 → Skript 动态市场队列（虚拟库存 / 物价）。
 * 行格式：materialId|amount|side  （sell=旧方块进入市场；buy=新方块从市场取出）
 */
final class MarketBridge {

    private static final Map<UUID, List<String>> lastBatchByPlayer = new ConcurrentHashMap<>();

    private MarketBridge() {
    }

    static void enqueue(Player player, FeeEstimate.Result estimate) {
        if (player == null || estimate == null || estimate.affectedBlocks() <= 0L) {
            return;
        }
        McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
        if (plugin == null || !plugin.getPluginConfig().getBoolean("record-market-stock", true)) {
            return;
        }
        List<String> lines = new ArrayList<>();
        collectLines(lines, estimate.removedCounts(), "sell");
        collectLines(lines, estimate.placedCounts(), "buy");
        if (lines.isEmpty()) {
            return;
        }
        lastBatchByPlayer.put(player.getUniqueId(), List.copyOf(lines));
        appendLines(plugin, lines);
    }

    /** //undo 时冲销上一笔创世神操作对应的市场变动，与方块还原对齐 */
    static void reverseLastBatch(Player player) {
        if (player == null) {
            return;
        }
        McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
        if (plugin == null || !plugin.getPluginConfig().getBoolean("record-market-stock", true)) {
            return;
        }
        if (!plugin.getPluginConfig().getBoolean("reverse-market-on-undo", true)) {
            return;
        }
        List<String> lines = lastBatchByPlayer.remove(player.getUniqueId());
        if (lines == null || lines.isEmpty()) {
            return;
        }
        List<String> reversed = new ArrayList<>(lines.size());
        for (String line : lines) {
            String[] parts = line.split("\\|", 3);
            if (parts.length < 3) {
                continue;
            }
            String side = parts[2].trim().toLowerCase();
            String opposite = "buy".equals(side) ? "sell" : "buy";
            reversed.add(parts[0] + "|" + parts[1] + "|" + opposite);
        }
        if (!reversed.isEmpty()) {
            appendLines(plugin, reversed);
        }
    }

    private static void collectLines(List<String> lines, Map<String, Long> counts, String side) {
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            String id = entry.getKey();
            long amount = entry.getValue();
            if (id == null || id.isBlank() || "air".equals(id) || amount <= 0L) {
                continue;
            }
            lines.add(sanitize(id) + "|" + amount + "|" + side);
        }
    }

    private static void appendLines(McwwsWeSurvivalPlugin plugin, List<String> lines) {
        String relative = plugin.getPluginConfig().getString(
                "market-trade-queue",
                "plugins/Skript/scripts/web/data/market_trade_queue.txt"
        );
        Path path = plugin.resolveDataFile(relative).toPath();
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            StringBuilder batch = new StringBuilder();
            for (String line : lines) {
                batch.append(line).append('\n');
            }
            Files.writeString(path, batch.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "写入市场库存队列失败: " + path, ex);
        }
    }

    private static String sanitize(String id) {
        return id.replace('|', '_').replace('\n', '_').replace('\r', '_');
    }
}
