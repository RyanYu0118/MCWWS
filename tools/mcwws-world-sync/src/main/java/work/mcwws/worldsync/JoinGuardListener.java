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
        String deny = plugin.admissionMessage();
        if (deny != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(deny, NamedTextColor.GOLD));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        if (plugin.config().allowBypassPermission && event.getPlayer().hasPermission("mcwws.worldsync.bypass")) {
            event.allow();
            return;
        }
        String deny = plugin.admissionMessage();
        if (deny != null) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text(deny, NamedTextColor.GOLD));
        }
    }
}
