package work.mcwws.worldedit;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public final class McwwsWeSurvivalPlugin extends JavaPlugin {

    private static McwwsWeSurvivalPlugin instance;
    private PriceCatalog priceCatalog;
    private WeSurvivalListener listener;
    private FileConfiguration config;
    private Set<String> chargeCommands = new HashSet<>();

    public static McwwsWeSurvivalPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadLocalConfig();
        if (!EconomyService.hook()) {
            getLogger().severe("未找到 Vault 经济服务，插件已禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        priceCatalog = new PriceCatalog(this);
        priceCatalog.reload();
        listener = new WeSurvivalListener(this);
        listener.register();
        getCommand("mcwws-we-reload").setExecutor((sender, command, label, args) -> {
            reloadLocalConfig();
            priceCatalog.reload();
            sendMessage(sender, color(config.getString("messages.prefix", "")) + "配置与价格已重载。");
            return true;
        });
        getLogger().info("生存创世神扣费已启用。");
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.unregister();
        }
    }

    public void reloadLocalConfig() {
        reloadConfig();
        config = getConfig();
        chargeCommands.clear();
        List<String> cmds = config.getStringList("charge-commands");
        if (cmds != null) {
            for (String cmd : cmds) {
                if (cmd != null && !cmd.isBlank()) {
                    chargeCommands.add(cmd.toLowerCase());
                }
            }
        }
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    public PriceCatalog getPriceCatalog() {
        return priceCatalog;
    }

    public Set<String> getChargeCommands() {
        return chargeCommands;
    }

    public String msg(String key) {
        return color(config.getString("messages." + key, key));
    }

    public String msg(String key, String... replacements) {
        String raw = msg(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return raw;
    }

    public static String color(String input) {
        if (input == null) {
            return "";
        }
        return input.replace('&', '§');
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    public static void sendMessage(Player player, String message) {
        sendMessage((CommandSender) player, message);
    }

    public File resolveDataFile(String relativePath) {
        return new File(relativePath);
    }

    public void saveResourceSilently(String resourcePath, File target) {
        if (target.getParentFile() != null) {
            target.getParentFile().mkdirs();
        }
        if (!target.exists()) {
            saveResource(resourcePath, false);
        }
    }

    public YamlConfiguration loadExternalYaml(File file) {
        if (file == null || !file.exists()) {
            return new YamlConfiguration();
        }
        try {
            return YamlConfiguration.loadConfiguration(file);
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "读取 YAML 失败: " + file.getPath(), ex);
            return new YamlConfiguration();
        }
    }
}
