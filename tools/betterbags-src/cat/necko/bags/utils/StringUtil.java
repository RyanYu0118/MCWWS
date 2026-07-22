/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.minimessage.MiniMessage
 */
package cat.necko.bags.utils;

import cat.necko.bags.bag.data.PlayerData;
import cat.necko.bags.utils.Placeholders;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class StringUtil {
    public static Component prepareFor(UUID uuid, String text) {
        return MiniMessage.miniMessage().deserialize((Object)Placeholders.replaceAll(uuid, text));
    }

    public static Component prepareFor(PlayerData data, String text) {
        return MiniMessage.miniMessage().deserialize((Object)Placeholders.replaceAll(data, text));
    }
}

