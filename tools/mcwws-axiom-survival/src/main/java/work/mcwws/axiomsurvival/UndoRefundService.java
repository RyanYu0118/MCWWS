package work.mcwws.axiomsurvival;

import org.bukkit.entity.Player;

import java.util.UUID;

/** 反向编辑（Axiom 撤销）退款：按创世神同一套规则退还原扣金额并扣手续费 */
final class UndoRefundService {

    private UndoRefundService() {
    }

    /** 退款成功返回实际到账金额，失败返回 0 */
    static double refund(Player player, ChargeHistory.Entry entry) {
        McwwsAxiomSurvivalPlugin plugin = McwwsAxiomSurvivalPlugin.getInstance();
        if (plugin == null || player == null || entry == null || entry.gross() <= 0D) {
            return 0D;
        }

        MarketBridge.reverse(entry.marketLines());

        double feeRate = plugin.getPluginConfig().getDouble("undo-refund.fee-rate", 0.05D);
        feeRate = Math.min(Math.max(feeRate, 0D), 1D);
        double gross = entry.gross();
        double fee = FeeAccumulator.round(gross * feeRate);
        double net = FeeAccumulator.round(gross - fee);
        if (net <= 0D) {
            return 0D;
        }

        String refId = "axiom-undo-" + UUID.randomUUID();
        String description = "Axiom 撤销退款: " + entry.label()
                + " (原扣 " + EconomyService.format(gross)
                + " 手续费 " + EconomyService.format(fee) + ")";
        if (!LedgerBridge.deposit(player, net, description, refId)) {
            McwwsAxiomSurvivalPlugin.sendMessage(player,
                    plugin.msg("prefix") + plugin.msg("undo-refund-failed"));
            return 0D;
        }
        return net;
    }
}
