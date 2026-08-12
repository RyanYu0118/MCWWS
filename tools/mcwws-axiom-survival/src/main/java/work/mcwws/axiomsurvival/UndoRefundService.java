package work.mcwws.axiomsurvival;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

/** 反向编辑（Axiom 撤销）资金冲销：按创世神同一套规则冲回原金额并收手续费 */
final class UndoRefundService {

    private UndoRefundService() {
    }

    /**
     * 冲销此前那笔编辑的资金。
     *
     * <p>原编辑净支出时退款到账（扣手续费）；原编辑净收入时（拆得比建得多）要把回收款连手续费一起收回。
     * 返回实际结算金额，正数为到账，负数为收回，0 表示没有可冲销的金额。
     */
    static double settle(Player player, ChargeHistory.Entry entry) {
        McwwsAxiomSurvivalPlugin plugin = McwwsAxiomSurvivalPlugin.getInstance();
        if (plugin == null || player == null || entry == null) {
            return 0D;
        }

        MarketBridge.reverse(entry.marketLines());

        double gross = entry.gross();
        if (Math.abs(gross) < 0.01D) {
            return 0D;
        }
        double feeRate = plugin.getPluginConfig().getDouble("undo-refund.fee-rate", 0.05D);
        feeRate = Math.min(Math.max(feeRate, 0D), 1D);
        double fee = FeeAccumulator.round(Math.abs(gross) * feeRate);

        String refId = "axiom-undo-" + UUID.randomUUID();
        if (gross > 0D) {
            double net = FeeAccumulator.round(gross - fee);
            if (net <= 0D) {
                return 0D;
            }
            String description = "Axiom 撤销退款: " + entry.label()
                    + " (原扣 " + EconomyService.format(gross)
                    + " 手续费 " + EconomyService.format(fee) + ")";
            if (!LedgerBridge.deposit(player, net, "axiom_undo", description, refId)) {
                McwwsAxiomSurvivalPlugin.sendMessage(player,
                        plugin.msg("prefix") + plugin.msg("undo-refund-failed"));
                return 0D;
            }
            return net;
        }

        double owed = FeeAccumulator.round(-gross + fee);
        String description = "Axiom 撤销收回回收款: " + entry.label()
                + " (原收 " + EconomyService.format(-gross)
                + " 手续费 " + EconomyService.format(fee) + ")";
        if (!LedgerBridge.withdraw(player, owed, "axiom_undo", description, refId)) {
            // 钱可能已经花掉，这里不拦撤销本身，只记一笔账面缺口
            plugin.getLogger().log(Level.WARNING, "撤销未能收回回收款: " + player.getName()
                    + " 应收 " + EconomyService.format(owed));
            return 0D;
        }
        return -owed;
    }
}
