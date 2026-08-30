package work.mcwws.immersivecreative;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class McwwsImmersiveCreativePlugin extends JavaPlugin {

    private ShopPriceIndex prices;
    private EconomyHook economy;
    private ImmersiveState state;
    private ImmersiveChannel channel;
    private CreativeSlotListener creativeSlots;
    private ImmersivePlaceholders placeholders;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        economy = new EconomyHook();
        if (!economy.hook()) {
            getLogger().severe("未找到 Vault 经济，插件已禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        prices = new ShopPriceIndex(this);
        prices.reload();
        state = new ImmersiveState(this);
        channel = new ImmersiveChannel(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, ImmersiveChannel.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, ImmersiveChannel.CHANNEL, channel);

        creativeSlots = new CreativeSlotListener(this);
        creativeSlots.register();
        getServer().getPluginManager().registerEvents(new JoinQuitListener(this), this);

        ImmersiveCommand command = new ImmersiveCommand(this);
        if (getCommand("mcwws-immersive") != null) {
            getCommand("mcwws-immersive").setExecutor(command);
        }
        if (getCommand("mcwws-immersive-reload") != null) {
            getCommand("mcwws-immersive-reload").setExecutor(command);
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholders = new ImmersivePlaceholders(this);
            placeholders.register();
        }

        getLogger().info("MCWWS_ImmersiveCreative 已启用。");
    }

    @Override
    public void onDisable() {
        if (creativeSlots != null) {
            creativeSlots.unregister();
        }
        if (placeholders != null) {
            placeholders.unregister();
        }
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, ImmersiveChannel.CHANNEL);
        getServer().getMessenger().unregisterIncomingPluginChannel(this, ImmersiveChannel.CHANNEL);
    }

    public ShopPriceIndex prices() {
        return prices;
    }

    public CreativeSlotListener creativeSlots() {
        return creativeSlots;
    }

    public boolean debug() {
        return getConfig().getBoolean("debug", true);
    }

    public EconomyHook economy() {
        return economy;
    }

    public ImmersiveState state() {
        return state;
    }

    public ImmersiveChannel channel() {
        return channel;
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

    public void send(CommandSender sender, String path, String... replacements) {
        String raw = color(getConfig().getString("messages.prefix", ""))
                + color(getConfig().getString(path, path));
        if (replacements != null) {
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(raw));
    }

    public static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }
}
