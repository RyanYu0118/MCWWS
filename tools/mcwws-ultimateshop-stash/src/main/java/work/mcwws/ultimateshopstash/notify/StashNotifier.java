package work.mcwws.ultimateshopstash.notify;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.Messages;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StashNotifier {

    private static final Map<UUID, Long> LAST_COLLECT_SOUND = new ConcurrentHashMap<>();

    private StashNotifier() {
    }

    public static void collect(McwwsUltimateShopStashPlugin plugin, Player player, String itemKey, long amount) {
        String display = Messages.displayMaterial(itemKey);
        String line = plugin.messages().legacy("collect-message", Map.of(
                "amount", String.valueOf(amount),
                "item", display
        ));
        String token = plugin.pendingReturns().register(player, itemKey, amount);

        TextComponent button = new TextComponent("[返回至背包]");
        button.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        button.setBold(true);
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mcwwsstash return " + token));
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("一次返回最近一分钟内入库的该种物品（操作栏 → 背包栏 → BetterBags）")
                        .color(net.md_5.bungee.api.ChatColor.GRAY).create()));
        BaseComponent[] prefix = TextComponent.fromLegacyText(line);
        BaseComponent[] message = new BaseComponent[prefix.length + 1];
        System.arraycopy(prefix, 0, message, 0, prefix.length);
        message[prefix.length] = button;
        player.spigot().sendMessage(message);

        playCollectSound(plugin, player);
    }

    private static void playCollectSound(McwwsUltimateShopStashPlugin plugin, Player player) {
        FileConfiguration cfg = plugin.getConfig();
        float volume = (float) cfg.getDouble("sounds.collect-volume", 0.15);
        if (volume <= 0f) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldownMs = cfg.getLong("sounds.collect-cooldown-ms", 800L);
        Long last = LAST_COLLECT_SOUND.get(player.getUniqueId());
        if (last != null && now - last < cooldownMs) {
            return;
        }
        LAST_COLLECT_SOUND.put(player.getUniqueId(), now);

        String soundName = cfg.getString("sounds.collect", "ENTITY_EXPERIENCE_ORB_PICKUP");
        Sound sound = null;
        try {
            sound = Registry.SOUNDS.get(NamespacedKey.minecraft(
                    soundName.toLowerCase(Locale.ROOT).replace('.', '_')));
        } catch (Throwable ignored) {
            // fall through to default
        }
        if (sound == null) {
            sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
        float pitch = (float) cfg.getDouble("sounds.collect-pitch", 1.2);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
