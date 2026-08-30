package work.mcwws.immersivecreative;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ImmersiveCommand implements CommandExecutor {

    private final McwwsImmersiveCreativePlugin plugin;

    public ImmersiveCommand(McwwsImmersiveCreativePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("mcwws-immersive-reload".equalsIgnoreCase(command.getName())) {
            if (!sender.hasPermission("mcwws.immersive-creative.admin")) {
                plugin.send(sender, "messages.no-permission");
                return true;
            }
            plugin.reloadConfig();
            plugin.prices().reload();
            plugin.send(sender, "messages.reloaded");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("请在游戏内执行。");
            return true;
        }
        if (!player.hasPermission("mcwws.immersive-creative.use")) {
            plugin.send(player, "messages.no-permission");
            return true;
        }
        boolean next;
        if (args.length >= 1 && "on".equalsIgnoreCase(args[0])) {
            next = true;
        } else if (args.length >= 1 && "off".equalsIgnoreCase(args[0])) {
            next = false;
        } else {
            next = !plugin.state().isEnabled(player);
        }
        plugin.state().setEnabled(player, next);
        if (!next && plugin.creativeSlots() != null) {
            plugin.creativeSlots().clearCredit(player.getUniqueId());
        }
        plugin.channel().sendState(player);
        plugin.send(player, next ? "messages.enabled" : "messages.disabled");
        if (next && !ImmersiveChannel.clientPresent(player)) {
            plugin.send(player, "messages.need-client");
        }
        return true;
    }
}
