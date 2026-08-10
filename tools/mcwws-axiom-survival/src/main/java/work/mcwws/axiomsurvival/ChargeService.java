package work.mcwws.axiomsurvival;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

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
        return player.getGameMode() == GameMode.SPECTATOR && AxiomPaperHook.isAxiomSessionActive(player);
    }

    public ChargeDecision evaluate(Player player, String label, FeeAccumulator.Result estimate) {
        if (!shouldCharge(player)) {
            return ChargeDecision.allow();
        }
        if (estimate == null) {
            return ChargeDecision.allow();
        }

        long maxScan = plugin.getPluginConfig().getLong("max-scan-blocks", 500000L);
        if (estimate.affectedBlocks() > maxScan) {
            notify(player, plugin.msg("scan-too-large", "max", String.valueOf(maxScan)));
            return ChargeDecision.deny("scan-too-large");
        }

        if (estimate.protectedBlocks() > 0L) {
            McwwsAxiomSurvivalPlugin.sendMessage(player, plugin.msg("prefix")
                    + "§c略过 §e" + estimate.protectedBlocks() + " §c个受保护方块（Slimefun/不可破坏），其余照常计费。");
        }

        if (estimate.affectedBlocks() <= 0L) {
            return ChargeDecision.allow();
        }

        double total = estimate.total();
        double balance = EconomyService.getBalance(player);
        if (total > balance + 1e-6) {
            notify(player, plugin.msg(
                    "insufficient-balance",
                    "total", EconomyService.format(total),
                    "blocks", String.valueOf(estimate.affectedBlocks()),
                    "demolition", EconomyService.format(estimate.demolition()),
                    "material", EconomyService.format(estimate.material()),
                    "labor", EconomyService.format(estimate.labor()),
                    "balance", EconomyService.format(balance)
            ));
            return ChargeDecision.deny("insufficient-balance");
        }

        MarketBridge.enqueue(player, estimate);

        if (total > 0D && !LedgerBridge.withdraw(player, total, label)) {
            notify(player, plugin.msg("prefix") + "扣款失败，请联系管理员。");
            return ChargeDecision.deny("withdraw-failed");
        }

        notify(player, plugin.msg(
                "charged",
                "total", EconomyService.format(total),
                "blocks", String.valueOf(estimate.affectedBlocks()),
                "demolition", EconomyService.format(estimate.demolition()),
                "material", EconomyService.format(estimate.material()),
                "labor", EconomyService.format(estimate.labor())
        ));
        return ChargeDecision.allow();
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
        if (AxiomPaperHook.isAxiomSessionActive(player)) {
            McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + plugin.msg("axiom-ready"));
        } else {
            McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + plugin.msg("axiom-inactive"));
        }
        if (BlockProtection.shouldBypass(player)) {
            McwwsAxiomSurvivalPlugin.sendMessage(player, prefix + "§e已启用扣费 bypass。");
        }
    }

    private static void notify(Player player, String message) {
        McwwsAxiomSurvivalPlugin.sendMessage(player, pluginPrefix(message));
    }

    private static String pluginPrefix(String message) {
        if (message.startsWith("§")) {
            return message;
        }
        McwwsAxiomSurvivalPlugin plugin = McwwsAxiomSurvivalPlugin.getInstance();
        return plugin == null ? message : plugin.msg("prefix") + message;
    }
}
