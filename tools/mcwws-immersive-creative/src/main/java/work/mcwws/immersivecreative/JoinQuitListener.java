package work.mcwws.immersivecreative;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

import java.util.UUID;

public final class JoinQuitListener implements Listener {

    private final McwwsImmersiveCreativePlugin plugin;

    public JoinQuitListener(McwwsImmersiveCreativePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 客户端注册通道时机不稳定：多拍几次，并靠 RegisterChannel / 客户端主动请求兜底
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> syncJoin(player, false), 20L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> syncJoin(player, false), 40L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> syncJoin(player, true), 100L);
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (!ImmersiveChannel.CHANNEL.equalsIgnoreCase(event.getChannel())) {
            return;
        }
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                plugin.channel().sendState(player);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.state().forget(uuid);
        plugin.creativeSlots().forget(uuid);
    }

    private void syncJoin(Player player, boolean warnIfMissingClient) {
        if (!player.isOnline()) {
            return;
        }
        plugin.channel().sendState(player);
        if (warnIfMissingClient
                && plugin.state().isEnabled(player)
                && !ImmersiveChannel.clientPresent(player)) {
            plugin.send(player, "messages.need-client");
        }
    }
}
