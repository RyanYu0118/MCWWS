package work.mcwws.ultimateshopstash.notify;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.Messages;

import java.util.Map;

public final class StashNotifier {

    private StashNotifier() {
    }

    public static void collect(McwwsUltimateShopStashPlugin plugin, Player player, String itemKey, long amount) {
        String display = Messages.displayMaterial(itemKey);
        String line = plugin.messages().legacy("collect-actionbar", Map.of(
                "amount", String.valueOf(amount),
                "item", display
        ));
        player.sendActionBar(line);

        TextComponent button = new TextComponent("[返回背包]");
        button.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        button.setBold(true);
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mcwwsstash exempt " + itemKey));
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("1 分钟内不再自动入库").color(net.md_5.bungee.api.ChatColor.GRAY).create()));
        player.spigot().sendMessage(button);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
    }
}
