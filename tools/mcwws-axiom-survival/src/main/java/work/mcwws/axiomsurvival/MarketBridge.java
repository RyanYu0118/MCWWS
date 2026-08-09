package work.mcwws.axiomsurvival;

import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

final class MarketBridge {

    private MarketBridge() {
    }

    static void enqueue(Player player, FeeAccumulator.Result estimate) {
        if (player == null || estimate == null || estimate.affectedBlocks() <= 0L) {
            return;
        }
        McwwsAxiomSurvivalPlugin plugin = McwwsAxiomSurvivalPlugin.getInstance();
        if (plugin == null || !plugin.getPluginConfig().getBoolean("record-market-stock", true)) {
            return;
        }
        List<String> lines = new ArrayList<>();
        collectLines(lines, estimate.removedCounts(), "sell");
        collectLines(lines, estimate.placedCounts(), "buy");
        if (lines.isEmpty()) {
            return;
        }
        appendLines(plugin, lines);
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

    private static void appendLines(McwwsAxiomSurvivalPlugin plugin, List<String> lines) {
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
