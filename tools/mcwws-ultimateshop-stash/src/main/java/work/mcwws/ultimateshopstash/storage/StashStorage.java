package work.mcwws.ultimateshopstash.storage;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import work.mcwws.ultimateshopstash.McwwsUltimateShopStashPlugin;
import work.mcwws.ultimateshopstash.util.Messages;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StashStorage {

    private final McwwsUltimateShopStashPlugin plugin;
    private final File dataDir;
    private final Map<UUID, PlayerStash> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Object> saveLocks = new ConcurrentHashMap<>();

    public StashStorage(McwwsUltimateShopStashPlugin plugin) {
        this.plugin = plugin;
        this.dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    public long getAmount(UUID uuid, String itemKey) {
        return getStash(uuid).getAmount(itemKey);
    }

    public long add(UUID uuid, String itemKey, long delta) {
        if (delta <= 0) {
            return getAmount(uuid, itemKey);
        }
        PlayerStash stash = getStash(uuid);
        long next = stash.add(itemKey, delta);
        saveAsync(uuid);
        return next;
    }

    public long remove(UUID uuid, String itemKey, long delta) {
        if (delta <= 0) {
            return getAmount(uuid, itemKey);
        }
        PlayerStash stash = getStash(uuid);
        long next = stash.remove(itemKey, delta);
        saveAsync(uuid);
        return next;
    }

    public boolean tryRemove(UUID uuid, String itemKey, long delta) {
        PlayerStash stash = getStash(uuid);
        if (!stash.tryRemove(itemKey, delta)) {
            return false;
        }
        saveAsync(uuid);
        return true;
    }

    public boolean isSkipCollect(UUID uuid, String itemKey) {
        return getStash(uuid).isSkipCollect(Messages.normalizeKey(itemKey));
    }

    public boolean toggleSkipCollect(UUID uuid, String itemKey) {
        boolean skipped = getStash(uuid).toggleSkipCollect(Messages.normalizeKey(itemKey));
        saveAsync(uuid);
        return skipped;
    }

    private PlayerStash getStash(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    private PlayerStash load(UUID uuid) {
        File file = new File(dataDir, uuid + ".yml");
        PlayerStash stash = new PlayerStash();
        if (!file.exists()) {
            return stash;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                long amount = section.getLong(key, 0L);
                if (amount > 0) {
                    stash.put(Messages.normalizeKey(key), amount);
                }
            }
        }
        for (String key : yaml.getStringList("skip-collect")) {
            if (key != null && !key.isBlank()) {
                stash.setSkipCollect(Messages.normalizeKey(key), true);
            }
        }
        return stash;
    }

    public void save(UUID uuid) {
        PlayerStash stash = cache.get(uuid);
        if (stash == null) {
            return;
        }
        File file = new File(dataDir, uuid + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Long> entry : stash.snapshot().entrySet()) {
            if (entry.getValue() > 0) {
                yaml.set("items." + entry.getKey(), entry.getValue());
            }
        }
        List<String> skipped = new ArrayList<>(stash.skipCollectSnapshot());
        if (!skipped.isEmpty()) {
            skipped.sort(String::compareTo);
            yaml.set("skip-collect", skipped);
        }
        try {
            writeYamlAtomically(file, yaml);
        } catch (IOException ex) {
            plugin.getLogger().warning("无法保存仓库 " + uuid + ": " + ex.getMessage());
        }
    }

    private void writeYamlAtomically(File file, YamlConfiguration yaml) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File tmp = new File(parent, file.getName() + ".tmp");
        IOException last = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                yaml.save(tmp);
                try {
                    Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (IOException ex) {
                last = ex;
                if (tmp.exists() && !tmp.delete()) {
                    tmp.deleteOnExit();
                }
                try {
                    Thread.sleep(50L * (attempt + 1));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("保存仓库被中断", interrupted);
                }
            }
        }
        // Windows 上目标 yml 常被编辑器/杀软占用内存映射，rename 会失败；退回原地覆盖。
        try {
            yaml.save(file);
            if (tmp.exists()) {
                tmp.delete();
            }
            return;
        } catch (IOException directEx) {
            if (last != null) {
                directEx.addSuppressed(last);
            }
            throw directEx;
        }
    }

    private void saveAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Object lock = saveLocks.computeIfAbsent(uuid, ignored -> new Object());
            synchronized (lock) {
                save(uuid);
            }
        });
    }

    public void saveAll() {
        for (UUID uuid : new HashMap<>(cache).keySet()) {
            save(uuid);
        }
    }

    public void mergeFrom(UUID uuid, String itemKey, long amount) {
        if (amount <= 0) {
            return;
        }
        add(uuid, Messages.normalizeKey(itemKey), amount);
    }
}
