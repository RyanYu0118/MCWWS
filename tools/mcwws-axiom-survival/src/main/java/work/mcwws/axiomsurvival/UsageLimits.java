package work.mcwws.axiomsurvival;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Axiom 建造额度：单次编辑上限、每日累计上限、每分钟格数上限。
 *
 * <p>Axiom 的一次编辑会拆成多包，因此不做「冷却」而做滑动窗口的格数限速，
 * 否则正常的大范围建造会被冷却截断。每日额度写入 {@code usage.yml}，重启不清零。
 */
final class UsageLimits {

    record Verdict(boolean allowed, String messageKey, String[] placeholders) {
        static final Verdict OK = new Verdict(true, null, new String[0]);

        static Verdict deny(String messageKey, String... placeholders) {
            return new Verdict(false, messageKey, placeholders);
        }
    }

    private record DailySpend(LocalDate date, double spent) {
    }

    private static final long RATE_WINDOW_MILLIS = 60_000L;

    private final McwwsAxiomSurvivalPlugin plugin;
    private final Map<UUID, DailySpend> daily = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<long[]>> recentBlocks = new ConcurrentHashMap<>();
    private volatile boolean dirty;

    UsageLimits(McwwsAxiomSurvivalPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    Verdict check(Player player, double total, long blocks) {
        if (player == null) {
            return Verdict.OK;
        }
        double perEdit = plugin.getPluginConfig().getDouble("limits.max-charge-per-edit", 0D);
        if (perEdit > 0D && total > perEdit + 1e-6) {
            return Verdict.deny("limit-per-edit",
                    "total", EconomyService.format(total),
                    "max", EconomyService.format(perEdit));
        }

        double dailyMax = plugin.getPluginConfig().getDouble("limits.daily-max-charge", 0D);
        if (dailyMax > 0D) {
            double spent = spentToday(player);
            if (spent + total > dailyMax + 1e-6) {
                return Verdict.deny("limit-daily",
                        "max", EconomyService.format(dailyMax),
                        "spent", EconomyService.format(spent));
            }
        }

        long ratePerMinute = plugin.getPluginConfig().getLong("limits.max-blocks-per-minute", 0L);
        if (ratePerMinute > 0L && blocks > 0L) {
            long used = blocksInWindow(player);
            if (used + blocks > ratePerMinute) {
                return Verdict.deny("limit-rate", "max", String.valueOf(ratePerMinute));
            }
        }
        return Verdict.OK;
    }

    void commit(Player player, double total, long blocks) {
        if (player == null) {
            return;
        }
        if (total > 0D) {
            UUID uuid = player.getUniqueId();
            LocalDate today = LocalDate.now();
            daily.compute(uuid, (key, current) -> current == null || !today.equals(current.date())
                    ? new DailySpend(today, total)
                    : new DailySpend(today, current.spent() + total));
            dirty = true;
        }
        if (blocks > 0L) {
            Deque<long[]> window = recentBlocks.computeIfAbsent(player.getUniqueId(), key -> new ArrayDeque<>());
            synchronized (window) {
                window.addLast(new long[]{System.currentTimeMillis(), blocks});
            }
        }
    }

    /** 撤销退款时归还当日额度 */
    void refund(Player player, double net) {
        if (player == null || net <= 0D) {
            return;
        }
        LocalDate today = LocalDate.now();
        daily.compute(player.getUniqueId(), (key, current) -> {
            if (current == null || !today.equals(current.date())) {
                return current;
            }
            return new DailySpend(today, Math.max(current.spent() - net, 0D));
        });
        dirty = true;
    }

    double spentToday(Player player) {
        DailySpend spend = daily.get(player.getUniqueId());
        if (spend == null || !LocalDate.now().equals(spend.date())) {
            return 0D;
        }
        return spend.spent();
    }

    private long blocksInWindow(Player player) {
        Deque<long[]> window = recentBlocks.get(player.getUniqueId());
        if (window == null) {
            return 0L;
        }
        long expireBefore = System.currentTimeMillis() - RATE_WINDOW_MILLIS;
        long sum = 0L;
        synchronized (window) {
            window.removeIf(entry -> entry[0] < expireBefore);
            for (long[] entry : window) {
                sum += entry[1];
            }
        }
        return sum;
    }

    void clear(Player player) {
        if (player != null) {
            recentBlocks.remove(player.getUniqueId());
        }
    }

    private File usageFile() {
        return new File(plugin.getDataFolder(), "usage.yml");
    }

    private void load() {
        File file = usageFile();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("daily");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                LocalDate date = LocalDate.parse(section.getString(key + ".date", ""));
                daily.put(UUID.fromString(key), new DailySpend(date, section.getDouble(key + ".spent", 0D)));
            } catch (Exception ignored) {
                // 坏行直接丢弃，额度按 0 起算
            }
        }
    }

    void save() {
        if (!dirty) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        LocalDate today = LocalDate.now();
        for (Map.Entry<UUID, DailySpend> entry : daily.entrySet()) {
            DailySpend spend = entry.getValue();
            if (!today.equals(spend.date()) || spend.spent() <= 0D) {
                continue;
            }
            yaml.set("daily." + entry.getKey() + ".date", spend.date().toString());
            yaml.set("daily." + entry.getKey() + ".spent", spend.spent());
        }
        try {
            File file = usageFile();
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            yaml.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "保存 Axiom 建造额度失败", ex);
        }
    }
}
