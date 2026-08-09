package work.mcwws.axiomsurvival;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public final class McwwsAxiomSurvivalPlugin extends JavaPlugin {

    private static McwwsAxiomSurvivalPlugin instance;
    private PriceCatalog priceCatalog;
    private ChargeService chargeService;
    private AxiomPaperHook axiomHook;
    private FileConfiguration config;

    public static McwwsAxiomSurvivalPlugin getInstance() {
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
        chargeService = new ChargeService(this);
        axiomHook = new AxiomPaperHook(this, chargeService);
        getServer().getPluginManager().registerEvents(new AxiomSurvivalListener(this, chargeService, axiomHook), this);
        getCommand("mcwws-axiom-reload").setExecutor((sender, command, label, args) -> {
            reloadLocalConfig();
            priceCatalog.reload();
            sendMessage(sender, color(config.getString("messages.prefix", "")) + "配置与价格已重载。");
            return true;
        });
        getCommand("axiomcheck").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, "请在游戏内使用。");
                return true;
            }
            chargeService.sendDiagnostic(player);
            return true;
        });
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!axiomHook.install()) {
                getLogger().warning("AxiomPaper 钩子安装失败，将在 5 秒后重试。");
                getServer().getScheduler().runTaskLater(this, () -> axiomHook.install(), 100L);
            }
        }, 20L);
        getLogger().info("MCWWS_AxiomSurvival 已启用。");
    }

    @Override
    public void onDisable() {
        axiomHook = null;
    }

    public void reloadLocalConfig() {
        reloadConfig();
        config = getConfig();
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    public PriceCatalog getPriceCatalog() {
        return priceCatalog;
    }

    public ChargeService getChargeService() {
        return chargeService;
    }

    public FeeAccumulator.LaborRates laborRates() {
        double place = config.getDouble("labor.place-unit", 0.5D);
        double demolish;
        if (config.contains("labor.demolish-unit")) {
            demolish = config.getDouble("labor.demolish-unit");
        } else {
            demolish = place * config.getDouble("labor.demolish-multiplier", 2.0D);
        }
        return new FeeAccumulator.LaborRates(place, demolish);
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
        return input == null ? "" : input.replace('&', '§');
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    public File resolveDataFile(String relativePath) {
        return new File(relativePath);
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
