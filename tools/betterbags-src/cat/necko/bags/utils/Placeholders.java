/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package cat.necko.bags.utils;

import cat.necko.bags.Plugin;
import cat.necko.bags.bag.data.PlayerData;
import cat.necko.bags.config.bags.data.BagLevel;
import java.util.UUID;
import org.bukkit.entity.Player;

public class Placeholders {
    public static String replaceAll(UUID uuidFor, String text) {
        PlayerData data = Plugin.getInstance().getPlayerData(uuidFor);
        return Placeholders.replaceAll(data, text);
    }

    public static String replaceAll(PlayerData data, String text) {
        BagLevel.Level level = data.getLevel();
        BagLevel.Level nextLevel = data.getNextLevel();
        if (text.contains("%player%")) {
            Player player = Plugin.getInstance().getServer().getPlayer(data.getUuid());
            String name = player == null ? "UNKNOWN" : player.getName();
            text = text.replace("%player%", name);
        }
        return text.replace("%uuid%", data.getUuid().toString()).replace("%items-sum%", Integer.toString(data.getItemsAmount())).replace("%current-level%", Integer.toString(data.getLevel().level())).replace("%current-capacity%", Integer.toString(level.capacity())).replace("%current-slots%", Integer.toString(level.slots())).replace("%current-cost%", Integer.toString(level.cost())).replace("%next-level%", Integer.toString(nextLevel.level())).replace("%next-capacity%", Integer.toString(nextLevel.capacity())).replace("%next-slots%", Integer.toString(nextLevel.slots())).replace("%next-cost%", Integer.toString(nextLevel.cost()));
    }
}

