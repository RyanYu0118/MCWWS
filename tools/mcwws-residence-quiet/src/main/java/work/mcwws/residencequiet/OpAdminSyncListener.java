package work.mcwws.residencequiet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;

/**
 * 指南菜单的「关闭管理员身份」走控制台 {@code deop}。Residence 只在进服时给 OP 打开 /resadmin，
 * 取消 OP 后开关仍留着，所以要在 op/deop 的下一 tick 立刻同步。
 */
final class OpAdminSyncListener implements Listener {

    private final McwwsResidenceQuietPlugin plugin;

    OpAdminSyncListener(McwwsResidenceQuietPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        scheduleSync(event.getCommand());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemoteCommand(RemoteServerCommandEvent event) {
        scheduleSync(event.getCommand());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        scheduleSync(event.getMessage());
    }

    private void scheduleSync(String raw) {
        String target = parseOpCommandTarget(raw);
        if (target == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayerExact(target);
            if (player == null) {
                player = Bukkit.getPlayer(target);
            }
            if (player == null) {
                return;
            }
            boolean admin = InteractGuardListener.syncResAdminToggle(player);
            if (plugin.guardDebug()) {
                plugin.getLogger().info("[guard-debug] op-sync " + player.getName() + " admin=" + admin);
            }
        });
    }

    /** @return op/deop 目标玩家名；无法识别则 null */
    static String parseOpCommandTarget(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        String head = parts[0];
        int colon = head.indexOf(':');
        if (colon >= 0) {
            head = head.substring(colon + 1);
        }
        if (!"op".equalsIgnoreCase(head) && !"deop".equalsIgnoreCase(head)) {
            return null;
        }
        return parts[1];
    }
}
