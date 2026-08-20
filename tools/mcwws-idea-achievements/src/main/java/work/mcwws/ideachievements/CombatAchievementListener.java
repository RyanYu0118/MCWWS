package work.mcwws.ideachievements;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.hm.achievement.category.MultipleAchievements;

public final class CombatAchievementListener implements Listener {

    private static final Set<Material> SPEARS = Set.of(
            Material.WOODEN_SPEAR,
            Material.STONE_SPEAR,
            Material.COPPER_SPEAR,
            Material.IRON_SPEAR,
            Material.GOLDEN_SPEAR,
            Material.DIAMOND_SPEAR,
            Material.NETHERITE_SPEAR);

    private final McwwsIdeaAchievementsPlugin plugin;

    public CombatAchievementListener(McwwsIdeaAchievementsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) {
            return;
        }

        // 倒反天罡：用长矛击杀服主
        if (victim.getUniqueId().equals(plugin.ownerUuid()) && isHoldingSpear(killer)) {
            plugin.aa().incrementCategoryForPlayer(
                    MultipleAchievements.CUSTOM,
                    plugin.ideaConfig().catOwnerSpear(),
                    killer,
                    1);
        }

        // 谁让你倒反天罡？：完成倒反后天被服主击杀
        if (killer.getUniqueId().equals(plugin.ownerUuid())
                && plugin.aa().hasPlayerReceivedAchievement(
                        victim.getUniqueId(), plugin.ideaConfig().ownerSpearAchievementName())) {
            if (!plugin.aa().hasPlayerReceivedAchievement(victim.getUniqueId(), "custom_owner_revenge_1")) {
                plugin.aa().incrementCategoryForPlayer(
                        MultipleAchievements.CUSTOM,
                        plugin.ideaConfig().catOwnerRevenge(),
                        victim,
                        1);
            }
        }
    }

    private static boolean isHoldingSpear(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return isSpear(main) || isSpear(off);
    }

    private static boolean isSpear(ItemStack stack) {
        return stack != null && SPEARS.contains(stack.getType());
    }
}
