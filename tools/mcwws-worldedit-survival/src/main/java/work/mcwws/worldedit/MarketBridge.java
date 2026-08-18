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
import java.util.logging.Level;

/**
 * 创世神方块变更 → Skript 动态市场队列（虚拟库存 / 物价）。
 * 行格式：materialId|amount|side  （sell=旧方块进入市场；buy=新方块从市场取出）
 */
final class MarketBridge {

    private MarketBridge() {
    }

    static List<String> linesOf(FeeEstimate.Result estimate) {
        if (estimate == null || estimate.affectedBlocks() <= 0L) {
            return List.of();
        }
        McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
        if (plugin == null || !plugin.getPluginConfig().getBoolean("record-market-stock", true)) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        collectLines(lines, estimate.netRemovedCounts(), "sell");
        collectLines(lines, estimate.netPlacedCounts(), "buy");
        return List.copyOf(lines);
    }

    static void write(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
        if (plugin == null) {
            return;
        }
        appendLines(plugin, lines);
    }

    static boolean writeReversed(List<String> lines) {
        McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
        if (plugin == null || !plugin.getPluginConfig().getBoolean("record-market-stock", true)) {
            return false;
        }
        if (!plugin.getPluginConfig().getBoolean("reverse-market-on-undo", true)) {
            return false;
        }
        List<String> reversed = reverseLines(lines);
        if (reversed.isEmpty()) {
            return false;
        }
        appendLines(plugin, reversed);
        return true;
    }

    private static List<String> reverseLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
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
        return reversed;
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
