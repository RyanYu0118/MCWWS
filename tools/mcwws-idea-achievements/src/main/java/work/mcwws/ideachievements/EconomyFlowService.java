package work.mcwws.ideachievements;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.hm.achievement.category.MultipleAchievements;

import work.mcwws.ideachievements.PlayerProgressStore.PlayerDayState;

public final class EconomyFlowService {

    private final McwwsIdeaAchievementsPlugin plugin;

    public EconomyFlowService(McwwsIdeaAchievementsPlugin plugin) {
        this.plugin = plugin;
    }

    /** 赚得（credit）计入日活；花费与赚得都计入亿万富翁流水。 */
    public void onMoneyFlow(UUID uuid, double amount, boolean credit) {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            // 离线时只持久化日活相关，等上线再推进 AA；流水需在线才能 increment
            if (credit) {
                rollAndAddEarn(uuid, amount, null);
                plugin.progressStore().save();
            }
            return;
        }

        int units = toPositiveInt(amount);
        if (units > 0) {
            plugin.aa().incrementCategoryForPlayer(
                    MultipleAchievements.CUSTOM,
                    plugin.ideaConfig().catEconomyFlow(),
                    player,
                    units);
        }

        if (credit) {
            rollAndAddEarn(uuid, amount, player);
            plugin.progressStore().save();
        }
    }

    public void onPlayerJoin(Player player) {
        rollDay(player.getUniqueId(), player, LocalDate.now(plugin.ideaConfig().zoneId()));
        plugin.progressStore().save();
    }

    private void rollAndAddEarn(UUID uuid, double amount, Player online) {
        LocalDate today = LocalDate.now(plugin.ideaConfig().zoneId());
        rollDay(uuid, online, today);
        PlayerDayState state = plugin.progressStore().get(uuid);
        if (state.trackingDate() == null) {
            state = new PlayerDayState(today, amount, state.streak(), state.darkChickenDone());
        } else if (state.trackingDate().equals(today)) {
            state = state.withEarned(state.dayEarned() + amount);
        } else {
            // rollDay 应已对齐到 today
            state = plugin.progressStore().get(uuid).withEarned(amount);
        }
        plugin.progressStore().put(uuid, state);
    }

    private void rollDay(UUID uuid, Player online, LocalDate today) {
        PlayerDayState state = plugin.progressStore().get(uuid);
        if (state.trackingDate() == null) {
            plugin.progressStore().put(uuid, state.withDate(today).withEarned(0));
            return;
        }
        if (!state.trackingDate().isBefore(today)) {
            return;
        }

        long gap = ChronoUnit.DAYS.between(state.trackingDate(), today);
        boolean success = state.dayEarned() >= plugin.ideaConfig().dailyEarnThreshold();
        int streak = state.streak();

        if (success) {
            streak++;
            if (online != null) {
                plugin.aa().incrementCategoryForPlayer(
                        MultipleAchievements.CUSTOM,
                        plugin.ideaConfig().catDailyActive(),
                        online,
                        1);
            }
        } else {
            streak = 0;
            resetDailyStat(online);
        }

        if (gap > 1) {
            streak = 0;
            resetDailyStat(online);
        }

        plugin.progressStore().put(uuid, new PlayerDayState(today, 0, streak, state.darkChickenDone()));
    }

    private void resetDailyStat(Player online) {
        if (online == null) {
            return;
        }
        if (plugin.aa().hasPlayerReceivedAchievement(online.getUniqueId(), "custom_daily_active_64")) {
            return;
        }
        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "aach reset Custom." + plugin.ideaConfig().catDailyActive() + " " + online.getName());
    }

    private static int toPositiveInt(double amount) {
        if (amount >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.floor(amount);
    }
}
