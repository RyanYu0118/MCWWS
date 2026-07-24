package work.mcwws.worldedit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.logging.Level;

/**
 * 创世神方块变更 → Skript 动态市场队列（虚拟库存 / 物价）。
 * 行格式：materialId|amount|buy  （sell=旧方块进入市场增库存；buy=新方块从市场取出减库存）
 */
final class MarketBridge {

    private MarketBridge() {
    }

    static void enqueue(FeeEstimate.Result estimate) {
        if (estimate == null || estimate.affectedBlocks() <= 0L) {
            return;
        }
        McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
        if (plugin == null || !plugin.getPluginConfig().getBoolean("record-market-stock", true)) {
            return;
        }
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
            appendSide(batch, estimate.removedCounts(), "sell");
            appendSide(batch, estimate.placedCounts(), "buy");
            if (batch.isEmpty()) {
                return;
            }
            Files.writeString(path, batch.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "写入市场库存队列失败: " + path, ex);
        }
    }

    private static void appendSide(StringBuilder batch, Map<String, Long> counts, String side) {
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            String id = entry.getKey();
            long amount = entry.getValue();
            if (id == null || id.isBlank() || "air".equals(id) || amount <= 0L) {
                continue;
            }
            batch.append(sanitize(id)).append('|').append(amount).append('|').append(side).append('\n');
        }
    }

    private static String sanitize(String id) {
        return id.replace('|', '_').replace('\n', '_').replace('\r', '_');
    }
}
