package work.mcwws.immersivecreative;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ImmersiveState {

    private final McwwsImmersiveCreativePlugin plugin;
    private final NamespacedKey key;
    private final Map<UUID, Boolean> cache = new ConcurrentHashMap<>();

    public ImmersiveState(McwwsImmersiveCreativePlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "enabled");
    }

    public boolean isEnabled(Player player) {
        if (player == null || !plugin.getConfig().getBoolean("enabled", true)) {
            return false;
        }
        if (!player.hasPermission("mcwws.immersive-creative.use")) {
            return false;
        }
        return cache.computeIfAbsent(player.getUniqueId(), uuid ->
                player.getPersistentDataContainer().getOrDefault(key, PersistentDataType.BYTE, (byte) 0) != 0);
    }

    public boolean toggle(Player player) {
        boolean next = !isEnabled(player);
        setEnabled(player, next);
        return next;
    }

    public void setEnabled(Player player, boolean enabled) {
        if (player == null) {
            return;
        }
        player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) (enabled ? 1 : 0));
        cache.put(player.getUniqueId(), enabled);
    }

    public void forget(UUID uuid) {
        cache.remove(uuid);
    }
}
