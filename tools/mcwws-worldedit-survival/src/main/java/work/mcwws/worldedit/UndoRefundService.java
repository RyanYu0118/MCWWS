package work.mcwws.worldedit;

import org.bukkit.entity.Player;

import java.util.UUID;

final class UndoRefundService {

    private UndoRefundService() {
    }

    static void handleUndo(Player player) {
        if (player == null) {
            return;
        }
        McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
        if (plugin == null) {
            return;
        }

        boolean marketReversed = MarketBridge.reverseLastBatch(player);

        if (!plugin.getPluginConfig().getBoolean("undo-refund.enabled", true)) {
            return;
        }

        WeChargeMemory.LastCharge charge = WeChargeMemory.take(player);
        if (charge == null || charge.grossAmount() <= 0D) {
            if (marketReversed) {
                McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("undo-market-only"));
            }
            return;
        }

        double gross = charge.grossAmount();
        double feeRate = plugin.getPluginConfig().getDouble("undo-refund.fee-rate", 0.05D);
        if (feeRate < 0D) {
            feeRate = 0D;
        }
        if (feeRate > 1D) {
            feeRate = 1D;
        }
        double fee = FeeEstimate.round(gross * feeRate);
        double net = FeeEstimate.round(gross - fee);
        if (net <= 0D) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("undo-refund-failed"));
            return;
        }

        String refId = "we-undo-" + UUID.randomUUID();
        String desc = "创世神撤销退款: " + charge.command()
                + " (原扣 " + EconomyService.format(gross)
                + " 手续费 " + EconomyService.format(fee) + ")";
        if (!LedgerBridge.deposit(player, net, desc, refId)) {
            WeChargeMemory.record(player, gross, charge.command());
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("undo-refund-failed"));
            return;
        }

        int percent = (int) Math.round(feeRate * 100D);
        McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                "undo-refunded",
                "gross", EconomyService.format(gross),
                "fee", EconomyService.format(fee),
                "percent", String.valueOf(percent),
                "net", EconomyService.format(net)
        ));
    }
}
