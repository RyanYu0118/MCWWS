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
import com.sk89q.worldedit.util.eventbus.EventHandler;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class WeSurvivalListener {

    private final McwwsWeSurvivalPlugin plugin;
    private final Object busSubscriber;

    public WeSurvivalListener(McwwsWeSurvivalPlugin plugin) {
        this.plugin = plugin;
        this.busSubscriber = new Object() {
            // 必须抢在 FAWE 的 PlatformCommandManager 之前：它收到 CommandEvent 就把指令丢去异步线程执行，
            // 排在后面的话预估还没跑完方块就已经写下去了。
            @Subscribe(priority = EventHandler.Priority.VERY_EARLY)
            public void onCommand(CommandEvent event) {
                handleCommand(event);
            }

            @Subscribe(priority = EventHandler.Priority.VERY_EARLY)
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
        Player player = resolvePlayer(actor);
        if (player == null || !BlockProtection.isSurvivalLike(player)) {
            return;
        }
        boolean bypass = BlockProtection.shouldBypass(player);

        String raw = event.getArguments();
        String command = FeeEstimate.rootCommand(raw);
        String root = command;
        if ("undo".equals(root) || "u".equals(root)) {
            WeEditAuthorization.allow(player);
            if (!bypass) {
                UndoRefundService.handleUndo(player);
            }
            return;
        }
        // //re 是 replace 别名；redo 只认 redo
        if ("redo".equals(root)) {
            WeEditAuthorization.allow(player);
            return;
        }
        if (isSelectionChangeCommand(root)) {
            WeRecentEditMemory.clear(player);
            return;
        }
        boolean setAirAlias = WeCommandAlias.isSetAirAlias(root);
        command = WeCommandAlias.canonical(root);
        if (!plugin.getChargeCommands().contains(command)) {
            return;
        }

        WeEditAuthorization.reset(player);

        if (plugin.getPluginConfig().getBoolean("reload-prices-before-estimate", true)) {
            plugin.getPriceCatalog().reload();
        }

        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        String[] args = FeeEstimate.splitArgs(raw);
        WeArgTokens tokens = WeArgTokens.parse(args);
        long maxScan = plugin.getPluginConfig().getLong("max-scan-blocks", 500000L);

        FeeEstimate.Result estimate;
        EstimateContext.setPlayer(player);
        try {
            if ("stack".equals(command)) {
                estimate = estimateStack(raw, session, event, actor, player, maxScan);
            } else if ("replacenear".equals(command)) {
                estimate = estimateReplaceNear(args, session, event, actor, player, maxScan);
            } else {
                estimate = estimateChargedCommand(command, tokens, setAirAlias, session, event, actor, player, maxScan);
            }
        } catch (InputParseException ex) {
            String key = ex.getMessage();
            if ("no-selection".equals(key)) {
                actor.printError(plugin.msg("no-selection"));
            } else if ("scan-too-large".equals(key)) {
                actor.printError(plugin.msg("scan-too-large", "max", String.valueOf(maxScan)));
            } else if ("empty-clipboard".equals(key)) {
                actor.printError(plugin.msg("empty-clipboard"));
            } else if ("need-cuboid".equals(key)) {
                actor.printError(plugin.msg("need-cuboid"));
            } else if ("need-convex".equals(key)) {
                actor.printError(plugin.msg("need-convex"));
            } else {
                actor.printError("无法解析方块参数: " + key);
            }
            event.setCancelled(true);
            WeEditAuthorization.deny(player);
            return;
        } catch (UnsupportedOperationException ex) {
            actor.printError(plugin.msg("unsupported-command"));
            event.setCancelled(true);
            WeEditAuthorization.deny(player);
            return;
        } finally {
            EstimateContext.clear();
        }

        if (estimate.residenceDeniedBlocks() > 0L) {
            actor.printError(plugin.msg(
                    "residence-denied",
                    "count", String.valueOf(estimate.residenceDeniedBlocks())
            ));
            event.setCancelled(true);
            WeEditAuthorization.deny(player);
            return;
        }

        if (bypass) {
            WeEditAuthorization.allow(player);
            return;
        }

        if (estimate.protectedBlocks() > 0) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("protected-present"));
        }

        if (requiresBlockChanges(command, tokens) && estimate.affectedBlocks() <= 0L) {
            actor.printError(plugin.msg("prefix") + plugin.msg("estimate-no-blocks"));
            event.setCancelled(true);
            WeEditAuthorization.deny(player);
            return;
        }

        double balance = EconomyService.getBalance(player);
        double total = estimate.total();
        // 拆除折现可能盖过材料与人工，此时这条指令是净收入，只有净支出才校验余额
        double charge = total > 0D ? FeeEstimate.round(total) : 0D;
        double payout = total < 0D ? FeeEstimate.round(-total) : 0D;
        if (charge > balance + 1e-6) {
            actor.printError(plugin.msg(
                    "insufficient-balance",
                    "total", EconomyService.format(charge),
                    "blocks", String.valueOf(estimate.affectedBlocks()),
                    "salvage", EconomyService.format(estimate.salvage()),
                    "material", EconomyService.format(estimate.material()),
                    "labor", EconomyService.format(estimate.labor()),
                    "balance", EconomyService.format(balance)
            ));
            event.setCancelled(true);
            WeEditAuthorization.deny(player);
            return;
        }

        if (estimate.affectedBlocks() > 0L) {
            MarketBridge.enqueue(player, estimate);
        }

        if (charge > 0D && !LedgerBridge.withdraw(player, charge, command)) {
            actor.printError(plugin.msg("prefix") + "扣款失败，请联系管理员。");
            event.setCancelled(true);
            WeEditAuthorization.deny(player);
            return;
        }
        if (payout > 0D && !LedgerBridge.deposit(player, payout, "worldedit_salvage",
                "创世神拆除回收: " + command, "we-salvage-" + java.util.UUID.randomUUID())) {
            actor.printError(plugin.msg("prefix") + "回收款入账失败，请联系管理员。");
            event.setCancelled(true);
            WeEditAuthorization.deny(player);
            return;
        }

        // 记带符号净额：撤销时正数退款、负数把回收款收回
        if (charge > 0D || payout > 0D) {
            WeChargeMemory.record(player, charge > 0D ? charge : -payout, command);
        }

        WeEditAuthorization.allow(player);
        if (estimate.affectedBlocks() > 0L) {
            recordRecentEdit(player, session, event, command, args, estimate);
        }

        if (payout > 0D) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                    "salvaged",
                    "total", EconomyService.format(payout),
                    "blocks", String.valueOf(estimate.affectedBlocks()),
                    "salvage", EconomyService.format(estimate.salvage()),
                    "material", EconomyService.format(estimate.material()),
                    "labor", EconomyService.format(estimate.labor())
            ));
        } else if (estimate.affectedBlocks() > 0L) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                    "charged",
                    "total", EconomyService.format(charge),
                    "blocks", String.valueOf(estimate.affectedBlocks()),
                    "salvage", EconomyService.format(estimate.salvage()),
                    "material", EconomyService.format(estimate.material()),
                    "labor", EconomyService.format(estimate.labor())
            ));
        }

        if (estimate.movedBlocks() > 0L) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                    "move-note",
                    "moved", String.valueOf(estimate.movedBlocks())
            ));
        }
    }

    private static boolean requiresBlockChanges(String command, WeArgTokens tokens) {
        if (("paste".equals(command) || "place".equals(command)) && tokens.has('n')) {
            return false;
        }
        return WeCommandAlias.CHARGE_CANONICAL.contains(command);
    }

    private static boolean isSelectionChangeCommand(String command) {
        return switch (command) {
            case "pos1", "pos2", "hpos1", "hpos2", "desel", "deselect", "sel" -> true;
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
        return FeeEstimate.forStack(plugin.getPriceCatalog(), plugin.laborRates(), region, world, stackArgs);
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
        return FeeEstimate.forReplaceNear(prices, plugin.laborRates(), world, center, radius, args[1], args[2], actor);
    }

    private FeeEstimate.Result estimateChargedCommand(
            String command,
            WeArgTokens tokens,
            boolean setAirAlias,
            LocalSession session,
            CommandEvent event,
            Actor actor,
            Player player,
            long maxScan
    ) throws InputParseException {
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

        Region region = null;
        if (WeShapeEstimate.needsSelection(command)) {
            try {
                region = session.getSelection(world);
            } catch (Exception ex) {
                throw new InputParseException("no-selection");
            }
            if (region == null) {
                throw new InputParseException("no-selection");
            }
        }

        return WeShapeEstimate.estimate(
                command,
                tokens,
                setAirAlias,
                plugin.getPriceCatalog(),
                plugin.laborRates(),
                session,
                actor,
                player,
                world,
                region,
                maxScan
        );
    }

    private void recordRecentEdit(Player player, LocalSession session, CommandEvent event, String command, String[] args, FeeEstimate.Result estimate) {
        if (player == null || session == null || estimate == null || estimate.placedCounts().isEmpty()) {
            return;
        }
        try {
            if ("replacenear".equals(command)) {
                if (args.length < 1) {
                    return;
                }
                int radius = Integer.parseInt(args[0]);
                World world = session.getSelectionWorld();
                if (world == null && event.getSession() != null) {
                    world = event.getSession().getWorld();
                }
                if (world == null) {
                    world = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(player.getWorld());
                }
                if (world == null) {
                    return;
                }
                Location loc = player.getLocation();
                BlockVector3 center = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                WeRecentEditMemory.recordBox(player, world, center, radius, estimate.placedCounts());
                return;
            }
            World world = session.getSelectionWorld();
            if (world == null && event.getSession() != null) {
                world = event.getSession().getWorld();
            }
            if (world == null) {
                return;
            }
            Region region = session.getSelection(world);
            if (region == null) {
                return;
            }
            WeRecentEditMemory.record(player, world, region, estimate.placedCounts());
        } catch (Exception ignored) {
        }
    }

    private void handleEditSession(EditSessionEvent event) {
        if (!plugin.getPluginConfig().getBoolean("enabled", true)) {
            return;
        }
        Actor actor = event.getActor();
        Player player = resolvePlayer(actor);
        // 扣费 bypass 仍要挂 Extent，才能拦无领地权限的写块
        if (player == null || !BlockProtection.isSurvivalLike(player)) {
            debug("EditSession 跳过: stage=" + event.getStage() + " actor=" + actorName(actor));
            return;
        }
        // FAWE 无视 CommandEvent 的取消，但 EditSessionEvent 一取消就会换成 NullExtent，
        // 这是唯一能整条掐掉「余额不足 / 缺领地权限」编辑的地方
        if (WeEditAuthorization.isDenied(player)) {
            event.setCancelled(true);
            debug("EditSession 已取消: stage=" + event.getStage() + " player=" + player.getName());
            return;
        }
        if (event.getStage() != EditSession.Stage.BEFORE_HISTORY) {
            return;
        }
        Extent current = event.getExtent();
        event.setExtent(new ProtectedFeeExtent(current, event.getWorld(), player));
        debug("EditSession 已挂闸门: player=" + player.getName() + " actor=" + actorName(actor));
    }

    /**
     * FAWE 执行指令时会把玩家包成 LocationMaskedPlayerWrapper，EditSessionEvent 里的 actor 不再是 BukkitPlayer，
     * 只按类型判断会漏掉所有编辑，必须按 UUID 反查。
     */
    private static Player resolvePlayer(Actor actor) {
        if (actor == null) {
            return null;
        }
        if (actor instanceof BukkitPlayer bukkitPlayer) {
            Player direct = bukkitPlayer.getPlayer();
            if (direct != null) {
                return direct;
            }
        }
        try {
            UUID uuid = actor.getUniqueId();
            return uuid == null ? null : Bukkit.getPlayer(uuid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String actorName(Actor actor) {
        return actor == null ? "null" : actor.getClass().getName();
    }

    private void debug(String message) {
        if (plugin.getPluginConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[debug] " + message);
        }
    }
}
