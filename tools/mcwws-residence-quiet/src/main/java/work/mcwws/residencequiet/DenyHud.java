package work.mcwws.residencequiet;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 每条拒绝提示一条 Boss 栏；进度条从满到空当作倒计时，归零后移除。
 */
final class DenyHud {

    private final McwwsResidenceQuietPlugin plugin;
    private final boolean bossBarEnabled;
    private final int durationTicks;
    private final int maxBars;
    private final BossBar.Color color;
    private final BossBar.Overlay overlay;
    private final Map<UUID, LinkedHashMap<String, Active>> bars = new LinkedHashMap<>();
    private BukkitTask ticker;

    private DenyHud(
            McwwsResidenceQuietPlugin plugin,
            boolean bossBarEnabled,
            int durationTicks,
            int maxBars,
            BossBar.Color color,
            BossBar.Overlay overlay
    ) {
        this.plugin = plugin;
        this.bossBarEnabled = bossBarEnabled;
        this.durationTicks = Math.max(20, durationTicks);
        this.maxBars = Math.max(1, maxBars);
        this.color = color;
        this.overlay = overlay;
    }

    static DenyHud fromConfig(McwwsResidenceQuietPlugin plugin, FileConfiguration config) {
        String display = config.getString("display", "bossbar");
        boolean boss = display == null || !"actionbar".equalsIgnoreCase(display.trim());
        int ticks = config.getInt("bossbar.duration-ticks", 80);
        int max = config.getInt("bossbar.max-bars", 6);
        BossBar.Color color = parseColor(config.getString("bossbar.color", "RED"));
        BossBar.Overlay overlay = parseOverlay(config.getString("bossbar.overlay", "PROGRESS"));
        return new DenyHud(plugin, boss, ticks, max, color, overlay);
    }

    boolean useBossBar() {
        return bossBarEnabled;
    }

    void start() {
        stopTicker();
        ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    void shutdown() {
        stopTicker();
        clearAll();
    }

    void show(Player player, String fingerprint, Component title) {
        if (!bossBarEnabled || player == null || fingerprint == null || fingerprint.isBlank()) {
            return;
        }
        UUID id = player.getUniqueId();
        LinkedHashMap<String, Active> bag = bars.computeIfAbsent(id, ignored -> new LinkedHashMap<>());
        Active existing = bag.get(fingerprint);
        if (existing != null && existing.remainingTicks > 0) {
            // 这条的倒计时还没走完：不叠第二条，也不把血量重新加满
            return;
        }
        if (existing != null) {
            bag.remove(fingerprint);
            player.hideBossBar(existing.bar);
        }
        while (bag.size() >= maxBars) {
            Iterator<Map.Entry<String, Active>> it = bag.entrySet().iterator();
            if (!it.hasNext()) {
                break;
            }
            Map.Entry<String, Active> oldest = it.next();
            it.remove();
            player.hideBossBar(oldest.getValue().bar);
        }
        BossBar bar = BossBar.bossBar(title, 1f, color, overlay);
        bag.put(fingerprint, new Active(bar, durationTicks));
        player.showBossBar(bar);
    }

    void clear(UUID playerId) {
        LinkedHashMap<String, Active> bag = bars.remove(playerId);
        if (bag == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        for (Active active : bag.values()) {
            if (player != null) {
                player.hideBossBar(active.bar);
            }
        }
    }

    void clearAll() {
        List<UUID> ids = new ArrayList<>(bars.keySet());
        for (UUID id : ids) {
            clear(id);
        }
    }

    private void tick() {
        if (bars.isEmpty()) {
            return;
        }
        List<UUID> gone = new ArrayList<>();
        for (Map.Entry<UUID, LinkedHashMap<String, Active>> playerEntry : bars.entrySet()) {
            Player player = Bukkit.getPlayer(playerEntry.getKey());
            LinkedHashMap<String, Active> bag = playerEntry.getValue();
            if (player == null || !player.isOnline()) {
                gone.add(playerEntry.getKey());
                continue;
            }
            Iterator<Map.Entry<String, Active>> it = bag.entrySet().iterator();
            while (it.hasNext()) {
                Active active = it.next().getValue();
                active.remainingTicks--;
                if (active.remainingTicks <= 0) {
                    player.hideBossBar(active.bar);
                    it.remove();
                    continue;
                }
                float progress = active.remainingTicks / (float) durationTicks;
                if (progress < 0f) {
                    progress = 0f;
                } else if (progress > 1f) {
                    progress = 1f;
                }
                active.bar.progress(progress);
            }
            if (bag.isEmpty()) {
                gone.add(playerEntry.getKey());
            }
        }
        for (UUID id : gone) {
            bars.remove(id);
        }
    }

    private void stopTicker() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    private static BossBar.Color parseColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return BossBar.Color.RED;
        }
        try {
            return BossBar.Color.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BossBar.Color.RED;
        }
    }

    private static BossBar.Overlay parseOverlay(String raw) {
        if (raw == null || raw.isBlank()) {
            return BossBar.Overlay.PROGRESS;
        }
        try {
            return BossBar.Overlay.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    private static final class Active {
        private final BossBar bar;
        private int remainingTicks;

        private Active(BossBar bar, int remainingTicks) {
            this.bar = bar;
            this.remainingTicks = remainingTicks;
        }
    }
}
