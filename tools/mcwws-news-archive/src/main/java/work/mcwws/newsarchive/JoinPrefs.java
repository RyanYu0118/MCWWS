package work.mcwws.newsarchive;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class JoinPrefs {
    public static final String ALWAYS_PERMISSION = "mcwws.newsarchive.join.always";

    public enum Mode {
        ON_UPDATE,
        ALWAYS;

        static Mode fromConfig(String raw) {
            if (raw == null) {
                return ON_UPDATE;
            }
            String normalized = raw.trim().toLowerCase().replace('-', '_');
            if (normalized.equals("always") || normalized.equals("every") || normalized.equals("every_join")) {
                return ALWAYS;
            }
            return ON_UPDATE;
        }
    }

    private final McwwsNewsArchivePlugin plugin;
    private final File file;
    private final Map<UUID, Mode> modes = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    private Mode defaultMode = Mode.ON_UPDATE;

    public JoinPrefs(McwwsNewsArchivePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "join-prefs.yml");
    }

    public synchronized void reload() {
        modes.clear();
        defaultMode = Mode.fromConfig(plugin.getConfig().getString("join.default-mode", "on_update"));
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                modes.put(uuid, Mode.fromConfig(players.getString(key)));
            } catch (IllegalArgumentException ignored) {
                // skip bad uuid
            }
        }
    }

    public Mode defaultMode() {
        return defaultMode;
    }

    public Mode modeOf(UUID uuid) {
        if (uuid == null) {
            return defaultMode;
        }
        return modes.getOrDefault(uuid, defaultMode);
    }

    public boolean isAlways(UUID uuid) {
        return modeOf(uuid) == Mode.ALWAYS;
    }

    public synchronized Mode toggle(UUID uuid) {
        Mode next = isAlways(uuid) ? Mode.ON_UPDATE : Mode.ALWAYS;
        if (next == defaultMode) {
            modes.remove(uuid);
        } else {
            modes.put(uuid, next);
        }
        save();
        return next;
    }

    public void applyPermission(Player player) {
        if (player == null) {
            return;
        }
        PermissionAttachment previous = attachments.remove(player.getUniqueId());
        if (previous != null) {
            previous.remove();
        }
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission(ALWAYS_PERMISSION, isAlways(player.getUniqueId()));
        attachments.put(player.getUniqueId(), attachment);
        player.updateCommands();
        player.recalculatePermissions();
    }

    public void clearPermission(Player player) {
        if (player == null) {
            return;
        }
        PermissionAttachment previous = attachments.remove(player.getUniqueId());
        if (previous != null) {
            previous.remove();
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Mode> entry : modes.entrySet()) {
            yaml.set("players." + entry.getKey(), entry.getValue() == Mode.ALWAYS ? "always" : "on_update");
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "写入进服弹书偏好失败", ex);
        }
    }
}
