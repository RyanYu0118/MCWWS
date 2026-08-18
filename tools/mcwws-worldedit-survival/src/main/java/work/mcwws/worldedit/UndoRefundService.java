package work.mcwws.worldedit;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class UndoRefundService {

    private UndoRefundService() {
    }

    /** @return false 时应取消本次 FAWE 历史操作，避免世界与资金不一致 */
    static boolean handleUndo(Player player, int times) {
        return handleHistory(player, times, true);
    }

    static boolean handleRedo(Player player, int times) {
        return handleHistory(player, times, false);
    }

    private static boolean handleHistory(Player player, int times, boolean undo) {
        if (player == null) {
            return true;
        }
        McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
        if (plugin == null) {
            return true;
        }

        List<WeChargeMemory.Entry> batch = undo
                ? WeChargeMemory.takeDone(player, times)
                : WeChargeMemory.takeUndone(player, times);
        if (batch.isEmpty()) {
            return true;
        }

        if (!plugin.getPluginConfig().getBoolean("undo-refund.enabled", true)) {
            applyMarket(batch, undo);
            if (undo) {
                WeChargeMemory.pushUndone(player, batch);
            } else {
                WeChargeMemory.pushDone(player, batch);
            }
            return true;
        }

        double feeRate = clampFeeRate(plugin.getPluginConfig().getDouble("undo-refund.fee-rate", 0.05D));
        if (!canAfford(player, batch, undo, feeRate)) {
            if (undo) {
                WeChargeMemory.restoreDone(player, batch);
                McwwsWeSurvivalPlugin.sendMessage(player,
                        plugin.msg("prefix") + plugin.msg("undo-clawback-failed"));
            } else {
                WeChargeMemory.restoreUndone(player, batch);
                McwwsWeSurvivalPlugin.sendMessage(player,
                        plugin.msg("prefix") + plugin.msg("redo-insufficient"));
            }
            return false;
        }

        for (int i = 0; i < batch.size(); i++) {
            WeChargeMemory.Entry entry = batch.get(i);
            if (!settle(player, plugin, entry, undo, feeRate)) {
                List<WeChargeMemory.Entry> remaining = new ArrayList<>(batch.subList(i, batch.size()));
                List<WeChargeMemory.Entry> settled = new ArrayList<>(batch.subList(0, i));
                if (undo) {
                    WeChargeMemory.restoreDone(player, remaining);
                    WeChargeMemory.pushUndone(player, settled);
                } else {
                    WeChargeMemory.restoreUndone(player, remaining);
                    WeChargeMemory.pushDone(player, settled);
                }
                return false;
            }
            applyMarket(List.of(entry), undo);
        }

        if (undo) {
            WeChargeMemory.pushUndone(player, batch);
        } else {
            WeChargeMemory.pushDone(player, batch);
        }
        return true;
    }

    private static boolean canAfford(Player player, List<WeChargeMemory.Entry> batch, boolean undo, double feeRate) {
        double needed = 0D;
        for (WeChargeMemory.Entry entry : batch) {
            double gross = entry.grossAmount();
            if (undo) {
                if (gross < 0D) {
                    needed += FeeEstimate.round(-gross + fee(gross, feeRate));
                }
            } else if (gross > 0D) {
                needed += FeeEstimate.round(gross);
            }
        }
        return needed <= EconomyService.getBalance(player) + 1e-6;
    }

    private static boolean settle(
            Player player,
            McwwsWeSurvivalPlugin plugin,
            WeChargeMemory.Entry entry,
            boolean undo,
            double feeRate
    ) {
        double gross = entry.grossAmount();
        if (Math.abs(gross) < 0.01D) {
            if (undo && entry.marketLines() != null && !entry.marketLines().isEmpty()) {
                McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("undo-market-only"));
            }
            return true;
        }

        double fee = fee(gross, feeRate);
        int percent = (int) Math.round(feeRate * 100D);
        String refId = (undo ? "we-undo-" : "we-redo-") + UUID.randomUUID();

        if (undo) {
            return settleUndo(player, plugin, entry, gross, fee, percent, refId);
        }
        return settleRedo(player, plugin, entry, gross, refId);
    }

    private static boolean settleUndo(
            Player player,
            McwwsWeSurvivalPlugin plugin,
            WeChargeMemory.Entry entry,
            double gross,
            double fee,
            int percent,
            String refId
    ) {
        if (gross < 0D) {
            double owed = FeeEstimate.round(-gross + fee);
            String desc = "创世神撤销收回回收款: " + entry.command()
                    + " (原收 " + EconomyService.format(-gross)
                    + " 手续费 " + EconomyService.format(fee) + ")";
            if (!LedgerBridge.withdraw(player, owed, "worldedit_undo", desc, refId)) {
                McwwsWeSurvivalPlugin.sendMessage(player,
                        plugin.msg("prefix") + plugin.msg("undo-clawback-failed"));
                return false;
            }
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                    "undo-clawback",
                    "gross", EconomyService.format(-gross),
                    "fee", EconomyService.format(fee),
                    "net", EconomyService.format(owed)
            ));
            return true;
        }

        double net = FeeEstimate.round(gross - fee);
        if (net <= 0D) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("undo-refund-failed"));
            return false;
        }
        String desc = "创世神撤销退款: " + entry.command()
                + " (原扣 " + EconomyService.format(gross)
                + " 手续费 " + EconomyService.format(fee) + ")";
        if (!LedgerBridge.deposit(player, net, "worldedit_undo", desc, refId)) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("undo-refund-failed"));
            return false;
        }
        McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                "undo-refunded",
                "gross", EconomyService.format(gross),
                "fee", EconomyService.format(fee),
                "percent", String.valueOf(percent),
                "net", EconomyService.format(net)
        ));
        return true;
    }

    private static boolean settleRedo(
            Player player,
            McwwsWeSurvivalPlugin plugin,
            WeChargeMemory.Entry entry,
            double gross,
            String refId
    ) {
        if (gross < 0D) {
            double payout = FeeEstimate.round(-gross);
            String desc = "创世神重做拆除回收: " + entry.command();
            if (!LedgerBridge.deposit(player, payout, "worldedit_salvage", desc, refId)) {
                McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("redo-failed"));
                return false;
            }
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                    "redo-salvaged",
                    "total", EconomyService.format(payout)
            ));
            return true;
        }

        double charge = FeeEstimate.round(gross);
        String desc = "创世神重做建造: " + entry.command();
        if (!LedgerBridge.withdraw(player, charge, "worldedit_redo", desc, refId)) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("redo-insufficient"));
            return false;
        }
        McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                "redo-charged",
                "total", EconomyService.format(charge)
        ));
        return true;
    }

    private static void applyMarket(List<WeChargeMemory.Entry> batch, boolean undo) {
        for (WeChargeMemory.Entry entry : batch) {
            if (entry.marketLines() == null || entry.marketLines().isEmpty()) {
                continue;
            }
            if (undo) {
                MarketBridge.writeReversed(entry.marketLines());
            } else {
                MarketBridge.write(entry.marketLines());
            }
        }
    }

    private static double fee(double gross, double feeRate) {
        return FeeEstimate.round(Math.abs(gross) * feeRate);
    }

    private static double clampFeeRate(double feeRate) {
        if (feeRate < 0D) {
            return 0D;
        }
        if (feeRate > 1D) {
            return 1D;
        }
        return feeRate;
    }
}
