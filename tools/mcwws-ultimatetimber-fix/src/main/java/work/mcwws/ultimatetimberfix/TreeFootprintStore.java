package work.mcwws.ultimatetimberfix;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public final class TreeFootprintStore {

    private static final BlockFace[] SCAN_FACES = {
            BlockFace.UP, BlockFace.DOWN,
            BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST
    };

    private final McwwsUltimateTimberFixPlugin plugin;
    private final File file;
    private final Set<String> protectedBlocks = new LinkedHashSet<>();

    public TreeFootprintStore(McwwsUltimateTimberFixPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "trees.yml");
    }

    public void load() {
        protectedBlocks.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> entries = config.getStringList("blocks");
        if (entries == null) {
            return;
        }
        for (String entry : entries) {
            if (entry != null && !entry.isBlank()) {
                protectedBlocks.add(entry);
            }
        }
        pruneInvalid();
        plugin.getLogger().info("已加载 " + protectedBlocks.size() + " 个 ExoticGarden 果树保护坐标。");
    }

    public void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("无法创建数据目录：" + plugin.getDataFolder());
            return;
        }
        FileConfiguration config = new YamlConfiguration();
        config.set("blocks", new ArrayList<>(protectedBlocks));
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("保存果树保护坐标失败：" + e.getMessage());
        }
    }

    public boolean contains(Location location) {
        return protectedBlocks.contains(key(location));
    }

    public void registerFromOrigin(Location origin) {
        if (origin == null || origin.getWorld() == null) {
            return;
        }
        Set<String> discovered = scanFootprint(origin);
        if (discovered.isEmpty()) {
            return;
        }
        int added = 0;
        for (String entry : discovered) {
            if (protectedBlocks.add(entry)) {
                added++;
            }
        }
        if (added > 0) {
            save();
            plugin.getLogger().fine("登记 ExoticGarden 果树 " + added + " 个方块 @" + format(origin));
        }
    }

    public void removeFootprint(Location origin) {
        if (origin == null) {
            return;
        }
        Set<String> discovered = scanFootprint(origin);
        if (discovered.isEmpty()) {
            return;
        }
        int removed = 0;
        for (String entry : discovered) {
            if (protectedBlocks.remove(entry)) {
                removed++;
            }
        }
        if (removed > 0) {
            save();
        }
    }

    private Set<String> scanFootprint(Location origin) {
        Set<String> discovered = new LinkedHashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        Block start = origin.getBlock();
        queue.add(start);
        int maxBlocks = plugin.footprintMaxBlocks();

        while (!queue.isEmpty() && discovered.size() < maxBlocks) {
            Block block = queue.poll();
            String blockKey = key(block.getLocation());
            if (!visited.add(blockKey)) {
                continue;
            }
            if (!SlimefunTreeDetector.isTreePart(block.getType())) {
                continue;
            }
            discovered.add(blockKey);
            for (BlockFace face : SCAN_FACES) {
                queue.add(block.getRelative(face));
            }
        }
        return discovered;
    }

    private void pruneInvalid() {
        int before = protectedBlocks.size();
        protectedBlocks.removeIf(entry -> {
            Location location = parse(entry);
            if (location == null || location.getWorld() == null) {
                return true;
            }
            return !SlimefunTreeDetector.isTreePart(location.getBlock().getType());
        });
        int removed = before - protectedBlocks.size();
        if (removed > 0) {
            save();
            plugin.getLogger().info("清理失效果树坐标 " + removed + " 个。");
        }
    }

    static String key(Location location) {
        return location.getWorld().getUID()
                + ":" + location.getBlockX()
                + ":" + location.getBlockY()
                + ":" + location.getBlockZ();
    }

    private static Location parse(String entry) {
        String[] parts = entry.split(":");
        if (parts.length != 4) {
            return null;
        }
        try {
            UUID worldId = UUID.fromString(parts[0]);
            var world = org.bukkit.Bukkit.getWorld(worldId);
            if (world == null) {
                return null;
            }
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String format(Location location) {
        return location.getWorld().getName()
                + ' ' + location.getBlockX()
                + ' ' + location.getBlockY()
                + ' ' + location.getBlockZ();
    }
}
