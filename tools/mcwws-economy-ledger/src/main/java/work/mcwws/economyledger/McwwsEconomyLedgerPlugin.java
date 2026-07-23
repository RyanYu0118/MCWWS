package work.mcwws.economyledger;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class McwwsEconomyLedgerPlugin extends JavaPlugin {

    private static McwwsEconomyLedgerPlugin instance;

    private LedgerQueueWriter queueWriter;
    private DedupCache dedupCache;
    private EssentialsBalanceListener balanceListener;

    private Map<String, String> causeCategories = Map.of();
    private Map<String, String> causeDescriptions = Map.of();
    private List<Double> flightAmounts = List.of(4.0D);
    private boolean flightSkipEnabled = true;
    private long dedupWindowMs = 4000L;

    public static McwwsEconomyLedgerPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadLocalConfig();

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("未找到 Vault，插件已禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        balanceListener = new EssentialsBalanceListener(this, queueWriter, dedupCache);
        balanceListener.register();

        if (getCommand("mcwws-ledger-reload") != null) {
            getCommand("mcwws-ledger-reload").setExecutor((sender, command, label, args) -> {
                reloadLocalConfig();
                balanceListener.probeEssentialsEvent();
                sendMessage(sender, "§a[MCWWS] 零钱明细经济层配置已重载。");
                return true;
            });
        }

        getLogger().info("零钱明细经济层兜底已启用，队列: " + getLedgerQueuePath());
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public void reloadLocalConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();

        Path queuePath = new File(config.getString("ledger-queue-path", "plugins/Skript/scripts/web/data/ledger_queue.txt"))
                .toPath();
        queueWriter = new LedgerQueueWriter(queuePath);
        dedupWindowMs = Math.max(config.getLong("dedup-window-ms", 4000L), 500L);
        dedupCache = new DedupCache(dedupWindowMs);

        flightSkipEnabled = config.getBoolean("flight.skip-via-economy-event", true);
        flightAmounts = config.getDoubleList("flight.amounts").stream()
                .map(value -> Math.round(value * 100.0) / 100.0)
                .toList();
        if (flightAmounts.isEmpty()) {
            flightAmounts = List.of(4.0D);
        }

        causeCategories = loadStringMap(config, "cause-categories");
        causeDescriptions = loadStringMap(config, "cause-descriptions");
    }

    private Map<String, String> loadStringMap(FileConfiguration config, String path) {
        if (config.getConfigurationSection(path) == null) {
            return Map.of();
        }
        Map<String, String> map = new HashMap<>();
        for (String key : config.getConfigurationSection(path).getKeys(false)) {
            String value = config.getString(path + "." + key, "");
            if (!value.isBlank()) {
                map.put(key.toUpperCase(Locale.ROOT), value.trim());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    public Path getLedgerQueuePath() {
        return new File(getConfig().getString("ledger-queue-path", "plugins/Skript/scripts/web/data/ledger_queue.txt"))
                .toPath();
    }

    public String categoryForCause(String cause) {
        return causeCategories.getOrDefault(cause.toUpperCase(Locale.ROOT), "other");
    }

    public String descriptionForCause(String cause, String direction) {
        String base = causeDescriptions.getOrDefault(cause.toUpperCase(Locale.ROOT), "经济变动");
        if ("credit".equals(direction) && !base.contains("收入") && !base.contains("转账")) {
            return base + "（收入）";
        }
        if ("debit".equals(direction) && !base.contains("支出") && !base.contains("扣")) {
            return base + "（支出）";
        }
        return base;
    }

    public boolean isFlightSkipEnabled() {
        return flightSkipEnabled;
    }

    public List<Double> getFlightAmounts() {
        return flightAmounts;
    }

    public long getDedupWindowMs() {
        return dedupWindowMs;
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null) {
            sender.sendMessage(message);
        }
    }

    public void logLedgerFailure(String message, Throwable error) {
        getLogger().log(Level.WARNING, message, error);
    }
}
