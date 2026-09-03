package work.mcwws.immersivecreative;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 玩家开关：内存缓存 + players.yml（主）+ 玩家 PDC（兼容旧数据）。
 * 仅靠 PDC 时，若进服状态包丢了或 player.dat 异常，重进会看起来像「又变回生存背包」。
 */
public final class ImmersiveState {

    private final McwwsImmersiveCreativePlugin plugin;
    private final NamespacedKey key;
    private final Map<UUID, Boolean> cache = new ConcurrentHashMap<>();
    private final File storeFile;
    private final FileConfiguration store;

    public ImmersiveState(McwwsImmersiveCreativePlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "enabled");
        this.storeFile = new File(plugin.getDataFolder(), "players.yml");
        this.store = YamlConfiguration.loadConfiguration(storeFile);
    }

    public boolean isEnabled(Player player) {
        if (player == null || !plugin.getConfig().getBoolean("enabled", true)) {
            return false;
        }
        if (!player.hasPermission("mcwws.immersive-creative.use")) {
            return false;
        }
        return cache.computeIfAbsent(player.getUniqueId(), uuid -> readStored(player));
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
        store.set(player.getUniqueId().toString(), enabled);
        saveStore();
    }

    public void forget(UUID uuid) {
        cache.remove(uuid);
    }

    private boolean readStored(Player player) {
        String id = player.getUniqueId().toString();
        if (store.contains(id)) {
            boolean fromFile = store.getBoolean(id);
            // 与 PDC 对齐，避免两处长期不一致
            byte pdc = player.getPersistentDataContainer().getOrDefault(key, PersistentDataType.BYTE, (byte) 0);
            if ((pdc != 0) != fromFile) {
                player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) (fromFile ? 1 : 0));
            }
            return fromFile;
        }
        boolean fromPdc = player.getPersistentDataContainer().getOrDefault(key, PersistentDataType.BYTE, (byte) 0) != 0;
        store.set(id, fromPdc);
        saveStore();
        return fromPdc;
    }

    private void saveStore() {
        try {
            File parent = storeFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("无法创建数据目录: " + parent);
            }
            store.save(storeFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "保存沉浸式创造开关失败", ex);
        }
    }
}
