package work.mcwws.ultimateshopstash.util;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.logging.Logger;

public final class Chat {

    private Chat() {
    }

    public static void send(CommandSender sender, Messages messages, String key, Map<String, String> vars) {
        String text = messages.legacy(key, vars);
        if (sender instanceof Player player) {
            player.spigot().sendMessage(TextComponent.fromLegacyText(text));
            return;
        }
        Logger logger = sender.getServer().getLogger();
        logger.info(ChatColor.stripColor(text));
    }
}
