package work.mcwws.ultimateshopstash.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.migrate.UltraDepositoryMigrator;
import work.mcwws.ultimateshopstash.util.Chat;
import work.mcwws.ultimateshopstash.util.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StashCommand implements CommandExecutor, TabCompleter {

    private final McwwsUltimateShopStashPlugin plugin;

    public StashCommand(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("exempt".equals(sub)) {
            if (!(sender instanceof Player player)) {
                return true;
            }
            if (!player.hasPermission("mcwwsstash.exempt")) {
                return true;
            }
            if (args.length < 2) {
                return false;
            }
            String key = Messages.normalizeKey(args[1]);
            plugin.exemptManager().grant(player, key);
            Chat.send(player, plugin.messages(), "collect-exempt-active", Map.of(
                    "item", Messages.displayMaterial(key),
                    "seconds", String.valueOf(plugin.exemptDurationSeconds())
            ));
            return true;
        }
        if ("migrate".equals(sub)) {
            if (!sender.hasPermission("mcwwsstash.admin")) {
                return true;
            }
            UltraDepositoryMigrator migrator = new UltraDepositoryMigrator(plugin);
            if (migrator.alreadyMigrated() && args.length < 2) {
                Chat.send(sender, plugin.messages(), "migrate-skipped", null);
                return true;
            }
            int players = migrator.migrate();
            Chat.send(sender, plugin.messages(), "migrate-done", Map.of("players", String.valueOf(players)));
            return true;
        }
        if ("reload".equals(sub)) {
            if (!sender.hasPermission("mcwwsstash.admin")) {
                return true;
            }
            plugin.reloadLocal();
            sender.getServer().getLogger().info("MCWWS_UltimateShopStash 已重载。");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            if ("exempt".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                out.add("exempt");
            }
            if (sender.hasPermission("mcwwsstash.admin")) {
                if ("migrate".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add("migrate");
                }
                if ("reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add("reload");
                }
            }
            return out;
        }
        if (args.length == 2 && "exempt".equalsIgnoreCase(args[0]) && sender instanceof Player) {
            List<String> out = new ArrayList<>();
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (String key : plugin.catalog().keys()) {
                if (key.startsWith(prefix)) {
                    out.add(key);
                }
            }
            return out;
        }
        return List.of();
    }
}
