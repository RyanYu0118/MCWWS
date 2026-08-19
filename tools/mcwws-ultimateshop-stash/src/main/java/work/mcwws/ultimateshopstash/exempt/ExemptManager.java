package work.mcwws.ultimateshopstash.exempt;

import org.bukkit.entity.Player;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.Messages;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ExemptManager {

    private final McwwsUltimateShopStashPlugin plugin;
    private final Map<UUID, Map<String, Long>> expiresAt = new ConcurrentHashMap<>();

    public ExemptManager(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isExempt(Player player, String itemKey) {
        if (player == null || itemKey == null) {
            return false;
        }
        Map<String, Long> perPlayer = expiresAt.get(player.getUniqueId());
        if (perPlayer == null) {
            return false;
        }
        Long until = perPlayer.get(Messages.normalizeKey(itemKey));
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            perPlayer.remove(Messages.normalizeKey(itemKey));
            return false;
        }
        return true;
    }

    public void grant(Player player, String itemKey) {
        long until = System.currentTimeMillis() + plugin.exemptDurationSeconds() * 1000L;
        expiresAt.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(Messages.normalizeKey(itemKey), until);
    }
}
