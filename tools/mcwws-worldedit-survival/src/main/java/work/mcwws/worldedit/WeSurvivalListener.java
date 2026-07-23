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
        if (!plugin.getChargeCommands().contains(command)) {
            return;
        }

        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        World world = session.getSelectionWorld();
        if (world == null && event.getSession() != null) {
            world = event.getSession().getWorld();
        }
        if (world == null) {
            actor.printError(plugin.msg("no-selection"));
            event.setCancelled(true);
            return;
        }

        Region region;
        try {
            region = session.getSelection(world);
        } catch (Exception ex) {
            actor.printError(plugin.msg("no-selection"));
            event.setCancelled(true);
            return;
        }
        if (region == null) {
            actor.printError(plugin.msg("no-selection"));
            event.setCancelled(true);
            return;
        }

        long maxScan = plugin.getPluginConfig().getLong("max-scan-blocks", 500000L);
        long volume = FeeEstimate.regionVolume(region);
        if (volume > maxScan) {
            actor.printError(plugin.msg("scan-too-large", "max", String.valueOf(maxScan)));
            event.setCancelled(true);
            return;
        }

        FeeEstimate.Result estimate;
        try {
            estimate = estimateCommand(command, FeeEstimate.splitArgs(raw), region, world, actor);
        } catch (InputParseException ex) {
            actor.printError("无法解析方块参数: " + ex.getMessage());
            event.setCancelled(true);
            return;
        } catch (UnsupportedOperationException ex) {
            actor.printError(plugin.msg("unsupported-command"));
            event.setCancelled(true);
            return;
        }

        if (estimate.protectedBlocks() > 0) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg("protected-present"));
        }

        double balance = EconomyService.getBalance(player);
        double total = estimate.total();
        if (total > balance + 1e-6) {
            actor.printError(plugin.msg(
                    "insufficient-balance",
                    "total", EconomyService.format(total),
                    "demolition", EconomyService.format(estimate.demolition()),
                    "material", EconomyService.format(estimate.material()),
                    "labor", EconomyService.format(estimate.labor()),
                    "balance", EconomyService.format(balance)
            ));
            event.setCancelled(true);
            return;
        }

        if (total > 0D && !LedgerBridge.withdraw(player, total, command)) {
            actor.printError(plugin.msg("prefix") + "扣款失败，请联系管理员。");
            event.setCancelled(true);
            return;
        }

        if (total > 0D) {
            McwwsWeSurvivalPlugin.sendMessage(player, plugin.msg("prefix") + plugin.msg(
                    "charged",
                    "total", EconomyService.format(total),
                    "demolition", EconomyService.format(estimate.demolition()),
                    "material", EconomyService.format(estimate.material()),
                    "labor", EconomyService.format(estimate.labor())
            ));
        }
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
            case "replacenear" -> {
                if (args.length < 3) {
                    throw new InputParseException("replacenear 参数不足");
                }
                yield FeeEstimate.forReplace(prices, region, world, args[1], args[2], actor);
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
        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) {
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
        Extent current = event.getExtent();
        event.setExtent(new ProtectedFeeExtent(current, event.getWorld()));
    }
}
