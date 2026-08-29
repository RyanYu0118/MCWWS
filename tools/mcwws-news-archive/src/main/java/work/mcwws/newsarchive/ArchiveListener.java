package work.mcwws.newsarchive;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;

public final class ArchiveListener implements Listener {
    private final McwwsNewsArchivePlugin plugin;

    public ArchiveListener(McwwsNewsArchivePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ArchiveGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory() != gui.getInventory()) {
            return;
        }
        int slot = event.getSlot();
        if (slot == 3) {
            if (gui.page() > 0) {
                ArchiveGui.open(plugin, player, gui.page() - 1);
            }
            return;
        }
        if (slot == 4) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.performCommand("dm open home"), 2L);
            return;
        }
        if (slot == 5) {
            ArchiveGui.open(plugin, player, gui.page() + 1);
            return;
        }
        if (slot == 8) {
            player.closeInventory();
            return;
        }
        String versionId = gui.versionIdAt(slot);
        if (versionId == null) {
            return;
        }
        NewsVersion version = plugin.store().get(versionId);
        if (version == null) {
            return;
        }
        plugin.store().markRead(player.getUniqueId(), versionId);
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.bookRenderer().open(player, version), 2L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ArchiveGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null) {
            return;
        }
        String body = message.startsWith("/") ? message.substring(1) : message;
        String lower = body.toLowerCase();
        if (lower.equals("news") || lower.startsWith("news ")) {
            markLatestRead(event.getPlayer());
            return;
        }
        if (lower.equals("booknews reload") || lower.startsWith("booknews reload ")) {
            Bukkit.getScheduler().runTaskLater(plugin, plugin::syncFromBookNewsQuiet, 20L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand();
        if (command == null) {
            return;
        }
        String lower = command.toLowerCase();
        if (lower.equals("booknews reload") || lower.startsWith("booknews reload ")) {
            Bukkit.getScheduler().runTaskLater(plugin, plugin::syncFromBookNewsQuiet, 20L);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("booknews.skip")) {
            return;
        }
        // 进服自动弹书后，把最新期标为已读（与 BookNews OpenBookDelaySecond 对齐）
        long delaySeconds = 5L;
        try {
            org.bukkit.configuration.file.YamlConfiguration cfg =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                            plugin.resolveServerFile(plugin.getConfig().getString(
                                    "booknews-config", "plugins/BookNews/config.yml")));
            if (!cfg.getBoolean("Open-Book-Onjoin.enable", true)) {
                return;
            }
            delaySeconds = Math.max(0L, cfg.getLong("OpenBookDelaySecond", 5L));
        } catch (Exception ignored) {
            // keep default
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> markLatestRead(player), (delaySeconds + 1L) * 20L);
    }

    private void markLatestRead(Player player) {
        NewsVersion latest = plugin.store().latest();
        if (latest != null) {
            plugin.store().markRead(player.getUniqueId(), latest.id());
        }
    }
}
