package work.mcwws.ultimateshopstash.shop;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;

import java.util.Locale;

public final class ShopReloadListener implements Listener {

    private final McwwsUltimateShopStashPlugin plugin;

    public ShopReloadListener(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isShopReload(event.getMessage())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.catalog().reload(), 20L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (isShopReload(event.getCommand())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.catalog().reload(), 20L);
        }
    }

    private static boolean isShopReload(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String command = raw.startsWith("/") ? raw.substring(1) : raw;
        command = command.toLowerCase(Locale.ROOT).trim();
        return command.equals("shop reload")
                || command.startsWith("shop reload ")
                || command.equals("ultimateshop reload")
                || command.startsWith("ultimateshop reload ");
    }
}
