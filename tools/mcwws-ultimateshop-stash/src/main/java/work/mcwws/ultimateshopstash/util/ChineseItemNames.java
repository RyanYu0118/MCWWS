package work.mcwws.ultimateshopstash.util;

import org.bukkit.Material;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * 从 UltimateShop 使用的 Minecraft 中文语言文件读取物品名称。
 */
public final class ChineseItemNames {

    private static Map<String, String> names = Map.of();

    private ChineseItemNames() {
    }

    public static void reload(McwwsUltimateShopStashPlugin plugin) {
        File file = new File(plugin.getDataFolder().getParentFile(), "UltimateShop/zh_cn.json");
        if (!file.isFile()) {
            names = Map.of();
            plugin.getLogger().warning("未找到 UltimateShop/zh_cn.json，仓库消息无法显示中文物品名。");
            return;
        }
        try {
            names = parseFlatJson(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            plugin.getLogger().info("已加载 " + names.size() + " 条中文物品名称。");
        } catch (Throwable throwable) {
            names = Map.of();
            plugin.getLogger().log(Level.WARNING, "读取中文物品名称失败。", throwable);
        }
    }

    public static String lookup(String itemKey) {
        String normalized = Messages.normalizeKey(itemKey);
        Material material = Material.matchMaterial(normalized.toUpperCase(Locale.ROOT));
        if (material != null) {
            String translationKey = material.translationKey();
            String translated = names.get(translationKey);
            if (translated != null && !translated.isBlank()) {
                return translated;
            }
        }

        for (String prefix : new String[] {"item.minecraft.", "block.minecraft."}) {
            String translated = names.get(prefix + normalized);
            if (translated != null && !translated.isBlank()) {
                return translated;
            }
        }
        return "未知物品";
    }

    private static Map<String, String> parseFlatJson(String text) {
        Map<String, String> result = new HashMap<>(8192);
        int index = 0;
        String pendingKey = null;
        while (index < text.length()) {
            if (text.charAt(index) != '"') {
                index++;
                continue;
            }
            StringBuilder token = new StringBuilder();
            index = readString(text, index + 1, token);
            if (pendingKey == null) {
                pendingKey = token.toString();
            } else {
                result.put(pendingKey, token.toString());
                pendingKey = null;
            }
        }
        return result;
    }

    private static int readString(String text, int index, StringBuilder out) {
        while (index < text.length()) {
            char current = text.charAt(index++);
            if (current == '"') {
                return index;
            }
            if (current != '\\') {
                out.append(current);
                continue;
            }
            if (index >= text.length()) {
                return index;
            }
            char escaped = text.charAt(index++);
            switch (escaped) {
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                case 'r' -> out.append('\r');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    if (index + 4 <= text.length()) {
                        out.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                        index += 4;
                    }
                }
                default -> out.append(escaped);
            }
        }
        return index;
    }
}
