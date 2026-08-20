package work.mcwws.ideachievements;

import java.time.ZoneId;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;

public final class IdeaConfig {

    private final UUID ownerUuid;
    private final String ownerName;
    private final double dailyEarnThreshold;
    private final ZoneId zoneId;
    private final String catOwnerSpear;
    private final String catEconomyFlow;
    private final String catDailyActive;
    private final String catOwnerRevenge;
    private final String catDarkChicken;
    private final String ownerSpearAchievementName;
    private final int darkChickenIntervalTicks;

    private IdeaConfig(
            UUID ownerUuid,
            String ownerName,
            double dailyEarnThreshold,
            ZoneId zoneId,
            String catOwnerSpear,
            String catEconomyFlow,
            String catDailyActive,
            String catOwnerRevenge,
            String catDarkChicken,
            String ownerSpearAchievementName,
            int darkChickenIntervalTicks) {
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.dailyEarnThreshold = dailyEarnThreshold;
        this.zoneId = zoneId;
        this.catOwnerSpear = catOwnerSpear;
        this.catEconomyFlow = catEconomyFlow;
        this.catDailyActive = catDailyActive;
        this.catOwnerRevenge = catOwnerRevenge;
        this.catDarkChicken = catDarkChicken;
        this.ownerSpearAchievementName = ownerSpearAchievementName;
        this.darkChickenIntervalTicks = darkChickenIntervalTicks;
    }

    public static IdeaConfig from(FileConfiguration cfg) {
        UUID owner = UUID.fromString(cfg.getString("owner-uuid", "d006ba70-4ddc-4b37-b13c-76f190815116"));
        ZoneId zone;
        try {
            zone = ZoneId.of(cfg.getString("timezone", "Asia/Shanghai"));
        } catch (Exception e) {
            zone = ZoneId.of("Asia/Shanghai");
        }
        return new IdeaConfig(
                owner,
                cfg.getString("owner-name", "Ryan_yu__"),
                cfg.getDouble("daily-earn-threshold", 9999),
                zone,
                cfg.getString("categories.owner-spear", "owner_spear"),
                cfg.getString("categories.economy-flow", "economy_flow"),
                cfg.getString("categories.daily-active", "daily_active"),
                cfg.getString("categories.owner-revenge", "owner_revenge"),
                cfg.getString("categories.dark-chicken", "dark_chicken"),
                cfg.getString("owner-spear-achievement-name", "custom_owner_spear_10"),
                cfg.getInt("dark-chicken-interval-ticks", 40));
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public String ownerName() {
        return ownerName;
    }

    public double dailyEarnThreshold() {
        return dailyEarnThreshold;
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public String catOwnerSpear() {
        return catOwnerSpear;
    }

    public String catEconomyFlow() {
        return catEconomyFlow;
    }

    public String catDailyActive() {
        return catDailyActive;
    }

    public String catOwnerRevenge() {
        return catOwnerRevenge;
    }

    public String catDarkChicken() {
        return catDarkChicken;
    }

    public String ownerSpearAchievementName() {
        return ownerSpearAchievementName;
    }

    public int darkChickenIntervalTicks() {
        return darkChickenIntervalTicks;
    }
}
