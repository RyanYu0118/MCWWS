package work.mcwws.worldedit;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/** Brush binding commands do not edit blocks and must not be charged.
 * FAWE 2.15 creates a session inside BrushTool.act for each actual click.
 * There is no brush-use event; inspect the creation call path rather than a
 * player-wide command authorization (which may belong to an earlier //set).
 */
final class BrushBilling {
    private BrushBilling() { }

    static boolean isBrushExecution() {
        return StackWalker.getInstance().walk(frames -> frames.anyMatch(frame ->
                frame.getClassName().equals("com.sk89q.worldedit.command.tool.BrushTool")
                        && frame.getMethodName().equals("act")));
    }

    static Extent wrap(Extent extent, World world, Player player, McwwsWeSurvivalPlugin plugin) {
        long max = plugin.getPluginConfig().getLong("max-scan-blocks", 500000L);
        return new BufferedBrushExtent(extent, max, (changes, tooLarge) -> {
            if (Bukkit.isPrimaryThread()) {
                return approve(changes, tooLarge, max, world, player, plugin);
            }
            // Vault, Residence, Slimefun and player messages use Bukkit's main thread.
            var payment = Bukkit.getScheduler().callSyncMethod(plugin,
                    () -> approve(changes, tooLarge, max, world, player, plugin));
            boolean interrupted = false;
            try {
                // Once queued, do not abandon payment: it might still run on
                // the server thread and charge a stroke we never replayed.
                for (;;) {
                    try {
                        return payment.get();
                    } catch (InterruptedException ex) {
                        interrupted = true;
                    }
                }
            } catch (ExecutionException ex) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "笔刷计费失败，已取消改块", ex.getCause());
                return false;
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private static boolean approve(Map<BlockVector3, BufferedBrushExtent.Change> changes,
                                   boolean tooLarge, long max, World world, Player player,
                                   McwwsWeSurvivalPlugin plugin) {
        if (!player.isOnline() || !plugin.isEnabled()) {
            return false;
        }
        if (tooLarge) {
            message(player, plugin, plugin.msg("scan-too-large", "max", String.valueOf(max)));
            return false;
        }
        long denied = 0;
        var iterator = changes.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (BlockProtection.isProtectedWorldBlock(world, entry.getKey())) {
                iterator.remove();
            } else if (!ResidenceProtection.canChange(player, world, entry.getKey(), entry.getValue().after())) {
                denied++;
            }
        }
        if (denied > 0) {
            message(player, plugin, plugin.msg("residence-denied", "count", String.valueOf(denied)));
            return false;
        }
        if (changes.isEmpty() || BlockProtection.shouldBypass(player)) {
            return true;
        }
        if (plugin.getPluginConfig().getBoolean("reload-prices-before-estimate", true)) {
            plugin.getPriceCatalog().reload();
        }
        FeeEstimate.ResultBuilder builder = new FeeEstimate.ResultBuilder(plugin.getPriceCatalog(), plugin.laborRates());
        changes.values().forEach(change -> builder.addChange(change.before(), change.after()));
        FeeEstimate.Result estimate = builder.build();
        double charge = Math.max(0, FeeEstimate.round(estimate.total()));
        double payout = Math.max(0, FeeEstimate.round(-estimate.total()));
        double balance = EconomyService.getBalance(player);
        if (charge > balance + 1e-6) {
            message(player, plugin, plugin.msg("insufficient-balance",
                    "total", EconomyService.format(charge),
                    "blocks", String.valueOf(estimate.affectedBlocks()),
                    "salvage", EconomyService.format(estimate.salvage()),
                    "material", EconomyService.format(estimate.material()),
                    "labor", EconomyService.format(estimate.labor()),
                    "balance", EconomyService.format(balance)));
            return false;
        }
        if (charge > 0 && !LedgerBridge.withdraw(player, charge, "brush")) {
            message(player, plugin, "笔刷扣款失败，已取消改块。");
            return false;
        }
        if (payout > 0 && !LedgerBridge.deposit(player, payout, "worldedit_salvage",
                "创世神拆除回收: brush", "we-brush-salvage-" + UUID.randomUUID())) {
            message(player, plugin, "笔刷回收款入账失败，已取消改块。");
            return false;
        }
        var marketLines = MarketBridge.linesOf(estimate);
        MarketBridge.write(marketLines);
        WeChargeMemory.record(player, charge > 0 ? charge : -payout, "brush", marketLines);
        WeRecentEditMemory.clear(player);
        message(player, plugin, plugin.msg(payout > 0 ? "salvaged" : "charged",
                "total", EconomyService.format(payout > 0 ? payout : charge),
                "blocks", String.valueOf(estimate.affectedBlocks()),
                "salvage", EconomyService.format(estimate.salvage()),
                "material", EconomyService.format(estimate.material()),
                "labor", EconomyService.format(estimate.labor())));
        return true;
    }

    private static void message(Player player, McwwsWeSurvivalPlugin plugin, String message) {
        McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + message);
    }
}
