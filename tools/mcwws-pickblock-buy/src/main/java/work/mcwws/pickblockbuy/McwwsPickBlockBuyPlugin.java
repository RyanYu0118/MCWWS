package work.mcwws.pickblockbuy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class McwwsPickBlockBuyPlugin extends JavaPlugin {

    private ShopMappingIndex mappingIndex;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        mappingIndex = new ShopMappingIndex(this);
        mappingIndex.reload();

        getServer().getPluginManager().registerEvents(new PickBlockBuyListener(this, mappingIndex), this);

        var reloadCommand = getCommand("mcwws-pickblock-reload");
        if (reloadCommand != null) {
            reloadCommand.setExecutor((sender, command, label, args) -> {
                if (!sender.hasPermission("mcwws.shop.pickbuy.admin")) {
                    sendLegacy(sender, color(getConfig().getString("messages.prefix", "")) + "§c缺少权限。");
                    return true;
                }
                reloadConfig();
                mappingIndex.reload();
                sendLegacy(sender, color(getConfig().getString("messages.prefix", "")) + "§a配置与映射已重载。");
                return true;
            });
        }

        getLogger().info("MCWWS_PickBlockBuy 已启用。");
    }

    public ShopMappingIndex getMappingIndex() {
        return mappingIndex;
    }

    public File resolveServerFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return new File(".");
        }
        File direct = new File(relativePath);
        if (direct.isAbsolute()) {
            return direct;
        }
        return new File(getDataFolder().getParentFile().getParentFile(), relativePath);
    }

    public long confirmTimeoutMillis() {
        return Math.max(1L, getConfig().getLong("confirm-timeout-seconds", 8L)) * 1000L;
    }

    public Component prefixComponent() {
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(getConfig().getString("messages.prefix", "&7[&e选块购买&7] "));
    }

    public String formatMessage(String path, String... replacements) {
        String raw = color(getConfig().getString(path, path));
        if (replacements != null) {
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return raw;
    }

    public static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    public static void sendLegacy(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
    }
}
