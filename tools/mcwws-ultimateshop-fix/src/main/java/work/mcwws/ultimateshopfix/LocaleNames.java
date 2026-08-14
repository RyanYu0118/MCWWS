package work.mcwws.ultimateshopfix;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 自己再读一遍 UltimateShop 的 Minecraft 语言文件，
 * 这样即使插件内部那份 LocateManager 没加载成功，也能拿到中文译名而不是翻译键。
 */
final class LocaleNames {

    private static Map<String, String> names = Map.of();

    private LocaleNames() {
    }

    static void reload() {
        Plugin shop = Bukkit.getPluginManager().getPlugin("UltimateShop");
        if (shop == null) {
            return;
        }
        File file = new File(shop.getDataFolder(), fileName(shop.getDataFolder()));
        if (!file.isFile()) {
            names = Map.of();
            McwwsUltimateShopFixPlugin.getInstance().getLogger()
                    .warning("未找到语言文件 " + file.getName() + "，物品译名将退回英文名。");
            return;
        }
        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            names = FlatJson.parse(text);
            McwwsUltimateShopFixPlugin.getInstance().getLogger()
                    .info("已加载 " + file.getName() + " 共 " + names.size() + " 条译名。");
        } catch (Throwable t) {
            names = Map.of();
            McwwsUltimateShopFixPlugin.getInstance().getLogger()
                    .log(Level.WARNING, "读取语言文件失败: " + file.getName(), t);
        }
    }

    static String lookup(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String value = names.get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String fileName(File dataFolder) {
        File config = new File(dataFolder, "config.yml");
        if (!config.isFile()) {
            return "zh_cn.json";
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(config);
            String name = yaml.getString("config-files.minecraft-locate-file.file");
            if (name != null && !name.isBlank()) {
                return name;
            }
        } catch (Throwable ignored) {
        }
        return "zh_cn.json";
    }

    /**
     * 语言文件是扁平的 {@code "键": "值"} 结构，这里手写扫描，免得依赖第三方 JSON 库。
     */
    private static final class FlatJson {

        private FlatJson() {
        }

        static Map<String, String> parse(String text) {
            Map<String, String> result = new HashMap<>(8192);
            int index = 0;
            int length = text.length();
            String pendingKey = null;
            while (index < length) {
                char c = text.charAt(index);
                if (c != '"') {
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

        private static int readString(String text, int start, StringBuilder out) {
            int index = start;
            int length = text.length();
            while (index < length) {
                char c = text.charAt(index++);
                if (c == '"') {
                    return index;
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (index >= length) {
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
                        if (index + 4 <= length) {
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
}
