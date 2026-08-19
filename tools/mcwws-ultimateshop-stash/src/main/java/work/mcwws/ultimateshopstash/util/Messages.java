package work.mcwws.ultimateshopstash.util;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Messages {

    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");

    private final McwwsUltimateShopStashPlugin plugin;
    private FileConfiguration config;

    public Messages(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public Component component(String key, Map<String, String> vars) {
        return Component.text(legacy(key, vars));
    }

    public String legacy(String key, Map<String, String> vars) {
        String raw = config.getString(key, key);
        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        Matcher matcher = HEX.matcher(raw);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + "x"
                    + serial(matcher.group(1).charAt(0))
                    + serial(matcher.group(1).charAt(1))
                    + serial(matcher.group(1).charAt(2))
                    + serial(matcher.group(1).charAt(3))
                    + serial(matcher.group(1).charAt(4))
                    + serial(matcher.group(1).charAt(5)));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    private static String serial(char c) {
        return String.valueOf(ChatColor.COLOR_CHAR) + Character.toLowerCase(c);
    }

    public static String normalizeKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public static String displayMaterial(String key) {
        return ChineseItemNames.lookup(key);
    }
}
