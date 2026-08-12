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
    private ChargeNotifier chargeNotifier;
    private ChargeHistory chargeHistory;
    private UsageLimits usageLimits;
    private EditorRestoreService editorRestoreService;
    private SurvivalEditorService survivalEditorService;
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
        chargeNotifier = new ChargeNotifier(this);
        chargeHistory = new ChargeHistory(this);
        usageLimits = new UsageLimits(this);
        chargeService = new ChargeService(this);
        editorRestoreService = new EditorRestoreService(this);
        survivalEditorService = new SurvivalEditorService(this, editorRestoreService);
        axiomHook = new AxiomPaperHook(this, chargeService, editorRestoreService, survivalEditorService);
        getServer().getMessenger().registerOutgoingPluginChannel(this, SurvivalEditorChannel.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(
                this,
                SurvivalEditorChannel.CHANNEL,
                new SurvivalEditorChannel(survivalEditorService)
        );
        getServer().getPluginManager().registerEvents(new AxiomSurvivalListener(this, chargeService, editorRestoreService, survivalEditorService), this);
        getServer().getPluginManager().registerEvents(new EditorVanillaMoveListener(editorRestoreService, survivalEditorService), this);
        getServer().getPluginManager().registerEvents(new SurvivalEditorJoinListener(this, survivalEditorService), this);
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
        getCommand("axiomrestore").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sendMessage(sender, "请在游戏内使用。");
                return true;
            }
            if (!player.hasPermission("mcwws.axiom.survival.use")) {
                sendMessage(sender, msg("prefix") + "§c缺少权限。");
                return true;
            }
            editorRestoreService.restoreNow(player);
            return true;
        });
        getServer().getScheduler().runTaskTimer(this, () -> usageLimits.save(), 1200L, 1200L);
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
        if (usageLimits != null) {
            usageLimits.save();
        }
        axiomHook = null;
    }

    public void reloadLocalConfig() {
        reloadConfig();
        config = getConfig();
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }

    /** 排查扣费链路时打开，会把每个编辑包的判定与预估结果写进控制台 */
    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }

    public PriceCatalog getPriceCatalog() {
        return priceCatalog;
    }

    public ChargeService getChargeService() {
        return chargeService;
    }

    ChargeNotifier getChargeNotifier() {
        return chargeNotifier;
    }

    ChargeHistory getChargeHistory() {
        return chargeHistory;
    }

    UsageLimits getUsageLimits() {
        return usageLimits;
    }

    public SurvivalEditorService getSurvivalEditorService() {
        return survivalEditorService;
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

    /** 拆除折现比例：卖价 × 该系数，1.0 等于完全按市场卖价回收 */
    public double salvageRate() {
        return Math.min(Math.max(config.getDouble("salvage.rate", 0.8D), 0D), 1D);
    }

    public boolean reloadPricesBeforeEstimate() {
        return config.getBoolean("reload-prices-before-estimate", true);
    }

    /** 实体操作劳务单价：无材料成本，只按只数计费 */
    public double entityUnit(String channel) {
        return switch (channel) {
            case "spawn_entity" -> config.getDouble("entity.spawn-unit", 20D);
            case "delete_entity" -> config.getDouble("entity.delete-unit", 5D);
            case "manipulate_entity" -> config.getDouble("entity.manipulate-unit", 2D);
            default -> 0D;
        };
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
