package work.mcwws.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class WeSurvivalListener {

    private final McwwsWeSurvivalPlugin plugin;
    private final Object busSubscriber;

    public WeSurvivalListener(McwwsWeSurvivalPlugin plugin) {
        this.plugin = plugin;
        this.busSubscriber = new Object() {
            @Subscribe
            public void onCommand(CommandEvent event) {
                handleCommand(event);
            }

            @Subscribe
            public void onEditSession(EditSessionEvent event) {
                handleEditSession(event);
            }
        };
    }

    public void register() {
        WorldEdit.getInstance().getEventBus().register(busSubscriber);
    }

    public void unregister() {
        WorldEdit.getInstance().getEventBus().unregister(busSubscriber);
    }

    private void handleCommand(CommandEvent event) {
        if (!plugin.getPluginConfig().getBoolean("enabled", true)) {
            return;
        }
        Actor actor = event.getActor();
        if (!(actor instanceof BukkitPlayer bukkitPlayer)) {
            return;
        }
        Player player = bukkitPlayer.getPlayer();
        if (player == null || !BlockProtection.isSurvivalLike(player) || BlockProtection.shouldBypass(player)) {
            return;
        }

        String raw = event.getArguments();
        String command = FeeEstimate.rootCommand(raw);
        if ("undo".equals(command) || "u".equals(command)) {
            WeEditAuthorization.grantHistory(player);
            UndoRefundService.handleUndo(player);
            return;
        }
        if ("redo".equals(command) || "re".equals(command)) {
            WeEditAuthorization.grantHistory(player);
            return;
        }
        if (!plugin.getChargeCommands().contains(command)) {
            return;
        }

        WeEditAuthorization.clear(player);

        if (plugin.getPluginConfig().getBoolean("reload-prices-before-estimate", true)) {
            plugin.getPriceCatalog().reload();
        }

        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        String[] args = FeeEstimate.splitArgs(raw);
        long maxScan = plugin.getPluginConfig().getLong("max-scan-blocks", 500000L);

        FeeEstimate.Result estimate;
        try {
            if ("replacenear".equals(command)) {
                estimate = estimateReplaceNear(args, session, event, actor, player, maxScan);
            } else if ("stack".equals(command)) {
                estimate = estimateStack(raw, session, event, actor, player, maxScan);
            } else {
                estimate = estimateSelectionCommand(command, args, session, event, actor, maxScan);
            }
        } catch (InputParseException ex) {
            String key = ex.getMessage();
            if ("no-selection".equals(key)) {
                actor.printError(plugin.msg("no-selection"));
            } else if ("scan-too-large".equals(key)) {
                actor.printError(plugin.msg("scan-too-large", "max", String.valueOf(maxScan)));
            } else {
                actor.printError("无法解析方块参数: " + key);
            }
            event.setCancelled(true);
            WeEditAuthorization.revokeUnpaid(player);
            return;
        } catch (UnsupportedOperationException ex) {
            actor.printError(plugin.msg("unsupported-command"));
            event.setCancelled(true);
            WeEditAuthorization.revokeUnpaid(player);
            return;
        }

        if (estimate.protectedBlocks() > 0) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("protected-present"));
        }

        if (requiresBlockChanges(command) && estimate.affectedBlocks() <= 0L) {
            actor.printError(plugin.msg("prefix") + plugin.msg("estimate-no-blocks"));
            event.setCancelled(true);
            WeEditAuthorization.revokeUnpaid(player);
            return;
        }

        double balance = EconomyService.getBalance(player);
        double total = estimate.total();
        if (total > balance + 1e-6) {
            actor.printError(plugin.msg(
                    "insufficient-balance",
                    "total", EconomyService.format(total),
                    "blocks", String.valueOf(estimate.affectedBlocks()),
                    "demolition", EconomyService.format(estimate.demolition()),
                    "material", EconomyService.format(estimate.material()),
                    "labor", EconomyService.format(estimate.labor()),
                    "balance", EconomyService.format(balance)
            ));
            event.setCancelled(true);
            WeEditAuthorization.revokeUnpaid(player);
            return;
        }

        if (estimate.affectedBlocks() > 0L) {
            MarketBridge.enqueue(player, estimate);
        }

        if (total > 0D && !LedgerBridge.withdraw(player, total, command)) {
            actor.printError(plugin.msg("prefix") + "扣款失败，请联系管理员。");
            event.setCancelled(true);
            WeEditAuthorization.revokeUnpaid(player);
            return;
        }

        if (total > 0D) {
            WeChargeMemory.record(player, total, command);
        }

        if (estimate.affectedBlocks() > 0L) {
            WeEditAuthorization.grantPaid(player, estimate.affectedBlocks());
        }

        if (total > 0D) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                    "charged",
                    "total", EconomyService.format(total),
                    "blocks", String.valueOf(estimate.affectedBlocks()),
                    "demolition", EconomyService.format(estimate.demolition()),
                    "material", EconomyService.format(estimate.material()),
                    "labor", EconomyService.format(estimate.labor())
            ));
        } else if (estimate.affectedBlocks() > 0L) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                    "charged",
                    "total", "0",
                    "blocks", String.valueOf(estimate.affectedBlocks()),
                    "demolition", "0",
                    "material", "0",
                    "labor", "0"
            ));
        }
    }

    private static boolean requiresBlockChanges(String command) {
        return switch (command) {
            case "set", "replace", "replacenear", "fill", "walls", "overlay", "repl", "stack" -> true;
            default -> false;
        };
    }

    private FeeEstimate.Result estimateStack(String raw, LocalSession session, CommandEvent event, Actor actor, Player player, long maxScan) throws InputParseException {
        World world = session.getSelectionWorld();
        if (world == null && event.getSession() != null) {
            world = event.getSession().getWorld();
        }
        if (world == null) {
            throw new InputParseException("no-selection");
        }

        Region region;
        try {
            region = session.getSelection(world);
        } catch (Exception ex) {
            throw new InputParseException("no-selection");
        }
        if (region == null) {
            throw new InputParseException("no-selection");
        }

        StackCommandArgs stackArgs = StackCommandArgs.parse(raw, player);
        if (stackArgs.scanVolume(region) > maxScan) {
            throw new InputParseException("scan-too-large");
        }
        return FeeEstimate.forStack(plugin.getPriceCatalog(), region, world, stackArgs);
    }

    private FeeEstimate.Result estimateReplaceNear(String[] args, LocalSession session, CommandEvent event, Actor actor, Player player, long maxScan) throws InputParseException {
        if (args.length < 3) {
            throw new InputParseException("replacenear 参数不足");
        }
        int radius;
        try {
            radius = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            throw new InputParseException("无效半径: " + args[0]);
        }
        if (radius < 0) {
            throw new InputParseException("半径不能为负数");
        }
        long volume = FeeEstimate.replaceNearScanVolume(radius);
        if (volume > maxScan) {
            throw new InputParseException("scan-too-large");
        }
        World world = session.getSelectionWorld();
        if (world == null && event.getSession() != null) {
            world = event.getSession().getWorld();
        }
        if (world == null) {
            world = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(player.getWorld());
        }
        if (world == null) {
            throw new InputParseException("无法确定世界");
        }
        Location loc = player.getLocation();
        BlockVector3 center = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        PriceCatalog prices = plugin.getPriceCatalog();
        return FeeEstimate.forReplaceNear(prices, world, center, radius, args[1], args[2], actor);
    }

    private FeeEstimate.Result estimateSelectionCommand(String command, String[] args, LocalSession session, CommandEvent event, Actor actor, long maxScan) throws InputParseException {
        World world = session.getSelectionWorld();
        if (world == null && event.getSession() != null) {
            world = event.getSession().getWorld();
        }
        if (world == null) {
            throw new InputParseException("no-selection");
        }

        Region region;
        try {
            region = session.getSelection(world);
        } catch (Exception ex) {
            throw new InputParseException("no-selection");
        }
        if (region == null) {
            throw new InputParseException("no-selection");
        }

        long volume = FeeEstimate.regionVolume(region);
        if (volume > maxScan) {
            throw new InputParseException("scan-too-large");
        }
        return estimateCommand(command, args, region, world, actor);
    }

    private FeeEstimate.Result estimateCommand(String command, String[] args, Region region, World world, Actor actor) throws InputParseException {
        PriceCatalog prices = plugin.getPriceCatalog();
        return switch (command) {
            case "set", "fill", "walls", "overlay", "repl" -> {
                if (args.length < 1) {
                    throw new InputParseException("缺少方块参数");
                }
                yield FeeEstimate.forSet(prices, region, world, joinArgs(args, 0), actor);
            }
            case "replace" -> {
                if (args.length < 2) {
                    throw new InputParseException("replace 需要两个方块参数");
                }
                yield FeeEstimate.forReplace(prices, region, world, args[0], args[1], actor);
            }
            default -> throw new UnsupportedOperationException(command);
        };
    }

    private static String joinArgs(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private void handleEditSession(EditSessionEvent event) {
        if (!plugin.getPluginConfig().getBoolean("enabled", true)) {
            return;
        }
        Actor actor = event.getActor();
        if (!(actor instanceof BukkitPlayer bukkitPlayer)) {
            return;
        }
        Player player = bukkitPlayer.getPlayer();
        if (player == null || !BlockProtection.isSurvivalLike(player) || BlockProtection.shouldBypass(player)) {
            return;
        }
        if (event.getStage() != EditSession.Stage.BEFORE_HISTORY) {
            return;
        }
        Extent current = event.getExtent();
        event.setExtent(new ProtectedFeeExtent(current, event.getWorld(), player));
    }
}
