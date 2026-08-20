package work.mcwws.ideachievements;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.hm.achievement.category.MultipleAchievements;

import work.mcwws.ideachievements.PlayerProgressStore.PlayerDayState;

public final class PlayerSessionListener implements Listener {

    private final McwwsIdeaAchievementsPlugin plugin;

    public PlayerSessionListener(McwwsIdeaAchievementsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.economyFlow().onPlayerJoin(event.getPlayer());
    }
}

final class DarkChickenTask implements Runnable {

    private final McwwsIdeaAchievementsPlugin plugin;

    DarkChickenTask(McwwsIdeaAchievementsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerDayState state = plugin.progressStore().get(player.getUniqueId());
            if (state.darkChickenDone()
                    || plugin.aa().hasPlayerReceivedAchievement(player.getUniqueId(), "custom_dark_chicken_1")) {
                continue;
            }
            for (Entity entity : player.getNearbyEntities(48, 48, 48)) {
                if (entity.getType() != EntityType.CHICKEN) {
                    continue;
                }
                if (!(entity instanceof LivingEntity living) || !living.isLeashed()) {
                    continue;
                }
                Entity holder = living.getLeashHolder();
                if (!(holder instanceof Player leashOwner) || !leashOwner.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                if (living.getLocation().getBlock().getLightLevel() != 0) {
                    continue;
                }
                if (!living.getLocation().getBlock().getRelative(0, -1, 0).getType().isAir()) {
                    continue;
                }
                plugin.aa().incrementCategoryForPlayer(
                        MultipleAchievements.CUSTOM,
                        plugin.ideaConfig().catDarkChicken(),
                        player,
                        1);
                plugin.progressStore().put(player.getUniqueId(), state.withDarkChickenDone(true));
                plugin.progressStore().save();
                break;
            }
        }
    }
}
