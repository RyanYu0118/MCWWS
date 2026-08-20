package work.mcwws.ideachievements;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class IdeaAchCommand implements CommandExecutor, TabCompleter {

    private final McwwsIdeaAchievementsPlugin plugin;

    public IdeaAchCommand(McwwsIdeaAchievementsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("用法: /mcwws-ideaach reload | money <玩家|UUID> <数额> [credit|debit]");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadLocal();
            sender.sendMessage("§a已重载 MCWWS_IdeaAchievements 配置。");
            return true;
        }
        if (args[0].equalsIgnoreCase("money")) {
            if (args.length < 3) {
                sender.sendMessage("用法: /mcwws-ideaach money <玩家|UUID> <数额> [credit|debit]");
                return true;
            }
            UUID uuid = resolveUuid(args[1]);
            if (uuid == null) {
                sender.sendMessage("§c找不到玩家或 UUID: " + args[1]);
                return true;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§c数额无效: " + args[2]);
                return true;
            }
            boolean credit = true;
            if (args.length >= 4) {
                String dir = args[3].toLowerCase(Locale.ROOT);
                if (dir.equals("debit") || dir.equals("spend") || dir.equals("out")) {
                    credit = false;
                }
            }
            plugin.economyFlow().onMoneyFlow(uuid, amount, credit);
            sender.sendMessage("§a已注入流水: " + (credit ? "credit" : "debit") + " " + amount + " → " + uuid);
            return true;
        }
        sender.sendMessage("未知子命令。用法: /mcwws-ideaach reload | money ...");
        return true;
    }

    private static UUID resolveUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
        }
        Player online = Bukkit.getPlayerExact(raw);
        if (online != null) {
            return online.getUniqueId();
        }
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(raw);
        return off.hasPlayedBefore() || off.isOnline() ? off.getUniqueId() : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "money").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("money")) {
            return List.of("credit", "debit");
        }
        return List.of();
    }
}
