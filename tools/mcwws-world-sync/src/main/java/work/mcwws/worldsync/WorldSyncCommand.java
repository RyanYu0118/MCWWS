package work.mcwws.worldsync;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class WorldSyncCommand implements CommandExecutor, TabCompleter {
    private final McwwsWorldSyncPlugin plugin;

    public WorldSyncCommand(McwwsWorldSyncPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mcwws.worldsync.admin")) {
            sender.sendMessage(Component.text("没有权限。", NamedTextColor.RED));
            return true;
        }
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status" -> {
                Map<String, Object> m = plugin.statusMap();
                sender.sendMessage(Component.text(JsonUtil.stringify(m), NamedTextColor.AQUA));
            }
            case "reload" -> {
                plugin.reloadSync();
                sender.sendMessage(Component.text("已重载 MCWWS_WorldSync 配置并重启监听。", NamedTextColor.GREEN));
            }
            case "flush" -> {
                if (!plugin.lock().hasLock()) {
                    sender.sendMessage(Component.text("本节点没有写入锁。", NamedTextColor.YELLOW));
                    return true;
                }
                plugin.flush().runNow(true, () -> plugin.getServer().getScheduler().runTask(plugin,
                        () -> sender.sendMessage(Component.text("冲刷完成。", NamedTextColor.GREEN))));
                sender.sendMessage(Component.text("正在冲刷脏文件…", NamedTextColor.AQUA));
            }
            case "claim" -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.claimLock(true);
                    sender.sendMessage(Component.text("已声明写入锁。", NamedTextColor.GREEN));
                } catch (Exception e) {
                    sender.sendMessage(Component.text("声明失败: " + e.getMessage(), NamedTextColor.RED));
                }
            });
            case "release" -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    if (plugin.config().s3Mode() && plugin.relay() != null) {
                        plugin.relay().release();
                    } else if (plugin.config().connectMode() && plugin.client() != null) {
                        plugin.client().release();
                    }
                    plugin.lock().releaseIfSelf();
                    plugin.lock().setJoiningBlocked(true);
                    plugin.getServer().getScheduler().runTask(plugin, () -> plugin.handover().applyStandbyWorldRules());
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> sender.sendMessage(Component.text("已释放写入锁，本端拒绝进服。", NamedTextColor.YELLOW)));
                } catch (Exception e) {
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> sender.sendMessage(Component.text("释放失败: " + e.getMessage(), NamedTextColor.RED)));
                }
            });
            case "handover" -> plugin.handover().requestTakeover(sender);
            case "push" -> plugin.forcePush().start(sender);
            case "force-lock" -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    if (plugin.config().s3Mode() && plugin.relay() != null) {
                        plugin.relay().force();
                    } else {
                        plugin.lock().setHolder(plugin.config().nodeId, plugin.lock().generation() + 1,
                                System.currentTimeMillis() + plugin.config().leaseSeconds * 1000L, false);
                        plugin.lock().setJoiningBlocked(false);
                    }
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.handover().restoreHolderWorldRules();
                        sender.sendMessage(Component.text("已强制本节点持锁（仅紧急排错；确认对端无人游玩）。", NamedTextColor.GOLD));
                    });
                } catch (Exception e) {
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> sender.sendMessage(Component.text("强制持锁失败: " + e.getMessage(), NamedTextColor.RED)));
                }
            });
            default -> sender.sendMessage(Component.text("用法: /worldsync [status|handover|push|flush|claim|release|force-lock|reload]", NamedTextColor.GRAY));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            return Stream.of("status", "handover", "push", "flush", "claim", "release", "force-lock", "reload")
                    .filter(s -> s.startsWith(p))
                    .toList();
        }
        return List.of();
    }
}
