package work.mcwws.axiomsurvival;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.List;

public final class ChargeService {

    public record ChargeDecision(boolean allowed, String reasonKey) {
        public static ChargeDecision allow() {
            return new ChargeDecision(true, null);
        }

        public static ChargeDecision deny(String reasonKey) {
            return new ChargeDecision(false, reasonKey);
        }
    }

    private final McwwsAxiomSurvivalPlugin plugin;

    public ChargeService(McwwsAxiomSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean shouldCharge(Player player) {
        if (player == null || !plugin.getPluginConfig().getBoolean("enabled", true)) {
            return false;
        }
        if (!player.hasPermission("mcwws.axiom.survival.use")) {
            return false;
        }
        if (BlockProtection.shouldBypass(player)) {
            return false;
        }
        if (BlockProtection.isSurvivalLike(player)) {
            return true;
        }
        SurvivalEditorService editorService = plugin.getSurvivalEditorService();
        if (editorService != null && editorService.isClientEditorSession(player)) {
            return true;
        }
        if (!AxiomPaperHook.isAxiomSessionActive(player)) {
            return false;
        }
        GameMode mode = player.getGameMode();
        return mode == GameMode.SPECTATOR || mode == GameMode.CREATIVE;
    }

    public ChargeDecision evaluate(Player player, String label, FeeAccumulator.Result estimate) {
        if (estimate == null) {
            return ChargeDecision.allow();
        }
        if (plugin.isDebug()) {
            plugin.getLogger().info("[debug] " + label + " 预估: 格数=" + estimate.affectedBlocks()
                    + ", 受保护=" + estimate.protectedBlocks()
                    + ", 领地拒绝=" + estimate.residenceDeniedBlocks()
                    + ", 搬运=" + estimate.movedBlocks()
                    + ", 最近=" + FeeAccumulator.formatDistance(estimate.minDistance())
                    + ", 回收=" + estimate.salvage()
                    + ", 材料=" + estimate.material()
                    + ", 劳务=" + estimate.labor()
                    + ", 合计=" + estimate.total()
                    + ", 扣费=" + shouldCharge(player));
        }

        // 领地权限与扣费 bypass 无关：有 bypass 也不能改别人的领地
        if (estimate.residenceDeniedBlocks() > 0L) {
            deny(player, "residence-denied", plugin.msg("prefix") + plugin.msg(
                    "residence-denied", FeeAccumulator.withNear(
                            estimate.minDistance(),
                            "count", String.valueOf(estimate.residenceDeniedBlocks())
                    )));
            return ChargeDecision.deny("residence-denied");
        }

        if (!shouldCharge(player)) {
            return ChargeDecision.allow();
        }

        long maxScan = plugin.getPluginConfig().getLong("max-scan-blocks", 500000L);
        if (estimate.affectedBlocks() > maxScan) {
            deny(player, "scan-too-large", plugin.msg("prefix")
                    + plugin.msg("scan-too-large", FeeAccumulator.withNear(
                            estimate.minDistance(), "max", String.valueOf(maxScan))));
            return ChargeDecision.deny("scan-too-large");
        }

        if (estimate.protectedBlocks() > 0L) {
            long cap = plugin.getPluginConfig().getLong("protection.max-restore-blocks", 4096L);
            if (ProtectedBlockGuard.enabled() && estimate.protectedBlocks() > cap) {
                deny(player, "protected-too-many", plugin.msg("prefix") + plugin.msg(
                        "protected-too-many", FeeAccumulator.withNear(
                                estimate.minDistance(), "count", String.valueOf(estimate.protectedBlocks()))));
                return ChargeDecision.deny("protected-too-many");
            }
            plugin.getChargeNotifier().addProtectedSkipped(player, estimate.protectedBlocks(), estimate.minDistance());
        }

        if (estimate.affectedBlocks() <= 0L) {
            return allowAndGuard(estimate);
        }

        ChargeHistory.Entry reversed = plugin.getChargeHistory().takeReverseMatch(player, estimate);
        if (reversed != null) {
            double settled = UndoRefundService.settle(player, reversed);
            if (settled > 0D) {
                plugin.getUsageLimits().refund(player, settled);
                plugin.getChargeNotifier().addRefund(player, reversed.gross(), settled, estimate.minDistance());
            } else if (settled < 0D) {
                plugin.getChargeNotifier().addUndoClawback(player, -reversed.gross(), -settled, estimate.minDistance());
            }
            return allowAndGuard(estimate);
        }

        double total = estimate.total();
        // 拆除折现可能盖过材料与人工，此时这笔编辑是净收入，只有净支出才校验余额与限额
        double charge = total > 0D ? FeeAccumulator.round(total) : 0D;
        double payout = total < 0D ? FeeAccumulator.round(-total) : 0D;
        double balance = EconomyService.getBalance(player);
        if (charge > balance + 1e-6) {
            deny(player, "insufficient-balance", plugin.msg("prefix") + plugin.msg(
                    "insufficient-balance",
                    FeeAccumulator.withNear(
                            estimate.minDistance(),
                            "total", EconomyService.format(charge),
                            "blocks", String.valueOf(estimate.affectedBlocks()),
                            "salvage", EconomyService.format(estimate.salvage()),
                            "material", EconomyService.format(estimate.material()),
                            "labor", EconomyService.format(estimate.labor()),
                            "balance", EconomyService.format(balance)
                    )
            ));
            return ChargeDecision.deny("insufficient-balance");
        }

        // 金额传净支出，格数照传：净收入的编辑仍要受每日格数上限约束
        UsageLimits.Verdict verdict = plugin.getUsageLimits().check(player, charge, estimate.affectedBlocks());
        if (!verdict.allowed()) {
            deny(player, verdict.messageKey(),
                    plugin.msg("prefix") + plugin.msg(
                            verdict.messageKey(),
                            FeeAccumulator.withNear(estimate.minDistance(), verdict.placeholders())));
            return ChargeDecision.deny(verdict.messageKey());
        }

        if (charge > 0D && !LedgerBridge.withdraw(player, charge, label)) {
            deny(player, "withdraw-failed", plugin.msg("prefix")
                    + "扣款失败，请联系管理员。"
                    + nearSuffix(estimate.minDistance()));
            return ChargeDecision.deny("withdraw-failed");
        }
        if (payout > 0D && !depositSalvage(player, payout, label)) {
            deny(player, "salvage-failed", plugin.msg("prefix")
                    + "回收款入账失败，请联系管理员。"
                    + nearSuffix(estimate.minDistance()));
            return ChargeDecision.deny("salvage-failed");
        }

        List<String> marketLines = MarketBridge.enqueue(player, estimate);
        plugin.getUsageLimits().commit(player, charge, estimate.affectedBlocks());
        plugin.getChargeHistory().record(player, estimate, total, label, marketLines);
        plugin.getChargeNotifier().addBlockCharge(player, estimate, total);
        return allowAndGuard(estimate);
    }

    /** 实体生成/删除/调整：无材料成本，只按只数收劳务费 */
    public ChargeDecision evaluateEntities(Player player, String label, String kind, long count, double minDistance) {
        if (!shouldCharge(player) || count <= 0L) {
            return ChargeDecision.allow();
        }
        if (!plugin.getPluginConfig().getBoolean("entity.charge-labor", true)) {
            return ChargeDecision.allow();
        }
        double fee = FeeAccumulator.round(count * plugin.entityUnit(label));
        ChargeDecision decision = chargeFlat(player, label, fee, minDistance);
        if (decision.allowed() && fee > 0D) {
            plugin.getChargeNotifier().addEntityCharge(player, kind, count, fee, minDistance);
        }
        return decision;
    }

    /** 生物群系画笔：不消耗材料，只按格收劳务费 */
    public ChargeDecision evaluateBiome(Player player, String label, long cells, double minDistance) {
        if (!shouldCharge(player) || cells <= 0L) {
            return ChargeDecision.allow();
        }
        if (!plugin.getPluginConfig().getBoolean("biome.charge-labor", true)) {
            return ChargeDecision.allow();
        }
        double fee = FeeAccumulator.round(cells * plugin.getPluginConfig().getDouble("biome.cell-unit", 0.5D));
        ChargeDecision decision = chargeFlat(player, label, fee, minDistance);
        if (decision.allowed() && fee > 0D) {
            plugin.getChargeNotifier().addBiomeCharge(player, cells, fee, minDistance);
        }
        return decision;
    }

    /** 世界时间 / 世界属性：生存下直接禁止，不做计价 */
    public ChargeDecision evaluateWorldControl(Player player, String configKey, String messageKey) {
        if (!shouldCharge(player)) {
            return ChargeDecision.allow();
        }
        if (!plugin.getPluginConfig().getBoolean(configKey, true)) {
            return ChargeDecision.allow();
        }
        deny(player, messageKey, plugin.msg("prefix") + plugin.msg(messageKey));
        return ChargeDecision.deny(messageKey);
    }

    private ChargeDecision chargeFlat(Player player, String label, double fee, double minDistance) {
        if (fee <= 0D) {
            return ChargeDecision.allow();
        }
        double balance = EconomyService.getBalance(player);
        if (fee > balance + 1e-6) {
            deny(player, "insufficient-balance-simple", plugin.msg("prefix") + plugin.msg(
                    "insufficient-balance-simple",
                    FeeAccumulator.withNear(
                            minDistance,
                            "total", EconomyService.format(fee),
                            "balance", EconomyService.format(balance)
                    )
            ));
            return ChargeDecision.deny("insufficient-balance");
        }
        UsageLimits.Verdict verdict = plugin.getUsageLimits().check(player, fee, 0L);
        if (!verdict.allowed()) {
            deny(player, verdict.messageKey(),
                    plugin.msg("prefix") + plugin.msg(
                            verdict.messageKey(),
                            FeeAccumulator.withNear(minDistance, verdict.placeholders())));
            return ChargeDecision.deny(verdict.messageKey());
        }
        if (!LedgerBridge.withdraw(player, fee, label)) {
            deny(player, "withdraw-failed", plugin.msg("prefix")
                    + "扣款失败，请联系管理员。"
                    + nearSuffix(minDistance));
            return ChargeDecision.deny("withdraw-failed");
        }
        plugin.getUsageLimits().commit(player, fee, 0L);
        return ChargeDecision.allow();
    }

    private boolean depositSalvage(Player player, double amount, String label) {
        return LedgerBridge.deposit(
                player,
                amount,
                "axiom_salvage",
                "Axiom 拆除回收: " + label,
                "axiom-salvage-" + java.util.UUID.randomUUID()
        );
    }

    private ChargeDecision allowAndGuard(FeeAccumulator.Result estimate) {
        ProtectedBlockGuard.scheduleRestore(estimate.protectedStates());
        return ChargeDecision.allow();
    }

    private void deny(Player player, String key, String message) {
        plugin.getChargeNotifier().deny(player, key, message);
    }

    private static String nearSuffix(double minDistance) {
        return " §7最近 §e" + FeeAccumulator.formatDistance(minDistance) + " 格§f。";
    }

    public void sendDiagnostic(Player player) {
        String prefix = plugin.msg("prefix");
        boolean survivalLike = BlockProtection.isSurvivalLike(player);
        boolean editorSpectator = player.getGameMode() == GameMode.SPECTATOR && AxiomPaperHook.isAxiomSessionActive(player);
        if (!survivalLike && !editorSpectator) {
            McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + plugin.msg("not-survival"));
            return;
        }
        if (!player.hasPermission("mcwws.axiom.survival.use")) {
            McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + "§c缺少权限 §emcwws.axiom.survival.use§c。");
            return;
        }
        double balance = EconomyService.getBalance(player);
        McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + "§7余额 §e" + EconomyService.format(balance));
        FeeAccumulator.LaborRates labor = plugin.laborRates();
        McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + "§7劳务费 §f放置 §e"
                + EconomyService.format(labor.placeUnit()) + "§7/格§f，拆除 §e"
                + EconomyService.format(labor.demolishUnit()) + "§7/格");
        McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + "§7拆除回收 §f按市场卖价 §e"
                + Math.round(plugin.salvageRate() * 100D) + "%§7 折现，同时增加市场库存");
        double dailyMax = plugin.getPluginConfig().getDouble("limits.daily-max-charge", 0D);
        if (dailyMax > 0D) {
            McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + "§7今日已用 §e"
                    + EconomyService.format(plugin.getUsageLimits().spentToday(player))
                    + " §7/ 上限 §e" + EconomyService.format(dailyMax));
        }
        if (AxiomPaperHook.isAxiomSessionActive(player)) {
            McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + plugin.msg("axiom-ready"));
        } else {
            McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + plugin.msg("axiom-inactive"));
        }
        if (BlockProtection.shouldBypass(player)) {
            McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + "§e已启用扣费 bypass。");
        }
    }
}
