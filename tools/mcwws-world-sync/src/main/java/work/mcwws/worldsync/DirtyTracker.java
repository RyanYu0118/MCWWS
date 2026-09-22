package work.mcwws.worldsync;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DirtyTracker {
    private final Set<String> dirty = ConcurrentHashMap.newKeySet();

    public void mark(String rel) {
        if (rel != null && !rel.isBlank()) {
            dirty.add(PathPolicy.posix(rel));
        }
    }

    public void markChunk(World world, Chunk chunk, Path serverRoot) {
        markChunk(world, chunk.getX(), chunk.getZ(), serverRoot);
    }

    public void markChunk(World world, int chunkX, int chunkZ, Path serverRoot) {
        Path worldFolder = world.getWorldFolder().toPath();
        int rx = chunkX >> 5;
        int rz = chunkZ >> 5;
        Path base = dimensionRoot(world, worldFolder);
        String[] sub = {"region", "entities", "poi"};
        for (String folder : sub) {
            Path file = base.resolve(folder).resolve("r." + rx + "." + rz + ".mca");
            try {
                mark(PathPolicy.relative(serverRoot, file));
            } catch (IllegalArgumentException ignored) {
                // world outside container
            }
        }
        try {
            mark(PathPolicy.relative(serverRoot, worldFolder.resolve("level.dat")));
        } catch (IllegalArgumentException ignored) {
        }
    }

    /** 26.2 stores terrain under dimensions/&lt;namespace&gt;/&lt;key&gt;/; older worlds use world/region. */
    static Path dimensionRoot(World world, Path worldFolder) {
        var key = world.getKey();
        Path modern = worldFolder.resolve("dimensions").resolve(key.getNamespace()).resolve(key.getKey());
        if (Files.isDirectory(modern)) {
            return modern;
        }
        return worldFolder;
    }

    public void markPlayer(UUID uuid, Path serverRoot, Path worldContainer) {
        String id = uuid.toString();
        markExisting(serverRoot, worldContainer.resolve("world/players/data/" + id + ".dat"));
        markExisting(serverRoot, worldContainer.resolve("world/players/data/" + id + ".dat_old"));
        markExisting(serverRoot, worldContainer.resolve("world/playerdata/" + id + ".dat"));
        markExisting(serverRoot, worldContainer.resolve("world/playerdata/" + id + ".dat_old"));
        markExisting(serverRoot, worldContainer.resolve("world/players/stats/" + id + ".json"));
        markExisting(serverRoot, worldContainer.resolve("world/stats/" + id + ".json"));
        markExisting(serverRoot, worldContainer.resolve("world/players/advancements/" + id + ".json"));
        markExisting(serverRoot, worldContainer.resolve("world/advancements/" + id + ".json"));
    }

    public void markPlayer(Player player, Path serverRoot, Path worldContainer) {
        markPlayer(player.getUniqueId(), serverRoot, worldContainer);
    }

    private void markExisting(Path serverRoot, Path file) {
        if (Files.isRegularFile(file)) {
            try {
                mark(PathPolicy.relative(serverRoot, file));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public List<String> snapshotAndClear() {
        List<String> list = new ArrayList<>(dirty);
        dirty.clear();
        return list;
    }

    public List<String> snapshot() {
        return new ArrayList<>(dirty);
    }

    public int size() {
        return dirty.size();
    }

    public void addAll(Iterable<String> rels) {
        for (String r : rels) {
            mark(r);
        }
    }
}
