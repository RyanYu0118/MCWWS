package work.mcwws.newsarchive;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ArchiveCommand implements CommandExecutor, TabCompleter {
    private final McwwsNewsArchivePlugin plugin;

    public ArchiveCommand(McwwsNewsArchivePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("控制台请使用 /newsarchive sync"));
                return true;
            }
            if (!player.hasPermission("mcwws.newsarchive.use")) {
                plugin.send(player, "messages.no-permission");
                return true;
            }
            ArchiveGui.open(plugin, player, 0);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "sync" -> {
                if (!sender.hasPermission("mcwws.newsarchive.admin")) {
                    plugin.send(sender, "messages.no-permission");
                    return true;
                }
                NewsVersion created = plugin.store().syncFromBookNews();
                if (created == null) {
                    plugin.send(sender, "messages.synced-same");
                } else {
                    plugin.send(sender, "messages.synced-new", "id", created.id());
                }
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("mcwws.newsarchive.admin")) {
                    plugin.send(sender, "messages.no-permission");
                    return true;
                }
                plugin.reloadAll();
                plugin.send(sender, "messages.reloaded");
                return true;
            }
            case "open" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("仅玩家可打开留档书本。"));
                    return true;
                }
                if (!player.hasPermission("mcwws.newsarchive.use")) {
                    plugin.send(player, "messages.no-permission");
                    return true;
                }
                if (args.length < 2) {
                    ArchiveGui.open(plugin, player, 0);
                    return true;
                }
                NewsVersion version = plugin.store().get(args[1]);
                if (version == null) {
                    plugin.send(player, "messages.unknown", "id", args[1]);
                    return true;
                }
                plugin.store().markRead(player.getUniqueId(), version.id());
                plugin.send(player, "messages.opened", "title", version.title());
                plugin.bookRenderer().open(player, version);
                return true;
            }
            default -> {
                sender.sendMessage(Component.text("/newsarchive [sync|reload|open <id>]"));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("sync", "reload", "open")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(option);
                }
            }
            return out;
        }
        if (args.length == 2 && "open".equalsIgnoreCase(args[0])) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (NewsVersion version : plugin.store().listNewestFirst()) {
                if (version.id().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(version.id());
                }
            }
        }
        return out;
    }
}
