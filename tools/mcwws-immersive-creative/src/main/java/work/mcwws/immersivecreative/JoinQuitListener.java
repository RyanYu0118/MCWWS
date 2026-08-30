package work.mcwws.immersivecreative;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class JoinQuitListener implements Listener {

    private final McwwsImmersiveCreativePlugin plugin;

    public JoinQuitListener(McwwsImmersiveCreativePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            plugin.channel().sendState(player);
            if (plugin.state().isEnabled(player) && !ImmersiveChannel.clientPresent(player)) {
                plugin.send(player, "messages.need-client");
            }
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.state().forget(event.getPlayer().getUniqueId());
    }
}
