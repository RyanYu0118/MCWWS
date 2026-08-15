package work.mcwws.residencequiet;

import com.bekvon.bukkit.residence.event.ResidenceChangedEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

final class ResidenceVisitListener implements Listener {

    private final McwwsResidenceQuietPlugin plugin;

    ResidenceVisitListener(McwwsResidenceQuietPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResidenceChanged(ResidenceChangedEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        // 进入、离开或换领地都算新一趟，同类拒绝提示可以再提醒一次
        plugin.throttle().resetVisit(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.throttle().resetVisit(event.getPlayer().getUniqueId());
        plugin.hud().clear(event.getPlayer().getUniqueId());
    }
}
