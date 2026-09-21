package work.mcwws.worldsync;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public final class JoinGuardListener implements Listener {
    private final McwwsWorldSyncPlugin plugin;

    public JoinGuardListener(McwwsWorldSyncPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (plugin.lock().hasLock() && !plugin.lock().joiningBlocked()) {
            return;
        }
        String msg = plugin.config().denyMessage.replace("{holder}",
                plugin.lock().holder().isEmpty() ? "无/冻结" : plugin.lock().holder());
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(msg, NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        if (plugin.lock().hasLock() && !plugin.lock().joiningBlocked()) {
            return;
        }
        if (plugin.config().allowBypassPermission && event.getPlayer().hasPermission("mcwws.worldsync.bypass")) {
            event.allow();
            return;
        }
        String msg = plugin.config().denyMessage.replace("{holder}",
                plugin.lock().holder().isEmpty() ? "无/冻结" : plugin.lock().holder());
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text(msg, NamedTextColor.RED));
    }
}
