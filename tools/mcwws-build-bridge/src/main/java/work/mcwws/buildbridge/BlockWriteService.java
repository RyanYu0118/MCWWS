package work.mcwws.buildbridge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class BlockWriteService {

    private final McwwsBuildBridgePlugin plugin;
    private final PlayerQueryService players;

    public BlockWriteService(McwwsBuildBridgePlugin plugin, PlayerQueryService players) {
        this.plugin = plugin;
        this.players = players;
    }

    public Map<String, Object> getBlock(String worldName, int x, int y, int z) {
        return sync(() -> {
            World world = players.resolveWorld(worldName);
            BlockData data = world.getBlockAt(x, y, z).getBlockData();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("world", world.getName());
            out.put("x", x);
            out.put("y", y);
            out.put("z", z);
            out.put("block", data.getAsString());
            return out;
        });
    }

    public Map<String, Object> setBlock(String worldName, int x, int y, int z, String block) {
        return sync(() -> {
            World world = players.resolveWorld(worldName);
            BlockData data = parseBlock(block);
            world.getBlockAt(x, y, z).setBlockData(data, false);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("world", world.getName());
            out.put("x", x);
            out.put("y", y);
            out.put("z", z);
            out.put("block", data.getAsString());
            return out;
        });
    }

    public Map<String, Object> fill(String worldName, int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        return sync(() -> {
            World world = players.resolveWorld(worldName);
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);
            long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
            int maxFill = plugin.getConfig().getInt("limits.max-fill-volume", 200000);
            if (volume > maxFill) {
                throw new IllegalArgumentException("Fill volume " + volume + " exceeds limit " + maxFill);
            }
            BlockData data = parseBlock(block);
            int changed = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        world.getBlockAt(x, y, z).setBlockData(data, false);
                        changed++;
                    }
                }
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("world", world.getName());
            out.put("changed", changed);
            out.put("block", data.getAsString());
            out.put("from", List.of(minX, minY, minZ));
            out.put("to", List.of(maxX, maxY, maxZ));
            return out;
        });
    }

    public Map<String, Object> setBlocks(String worldName, List<Object> blocks) {
        return sync(() -> {
            World world = players.resolveWorld(worldName);
            int maxBatch = plugin.getConfig().getInt("limits.max-batch-blocks", 50000);
            if (blocks.size() > maxBatch) {
                throw new IllegalArgumentException("Batch size " + blocks.size() + " exceeds limit " + maxBatch);
            }
            int changed = 0;
            for (Object raw : blocks) {
                Map<String, Object> item = JsonUtil.asObject(raw);
                int x = JsonUtil.i(item, "x");
                int y = JsonUtil.i(item, "y");
                int z = JsonUtil.i(item, "z");
                String block = JsonUtil.str(item, "block");
                if (block == null || block.isBlank()) {
                    throw new IllegalArgumentException("Each block entry needs block");
                }
                world.getBlockAt(x, y, z).setBlockData(parseBlock(block), false);
                changed++;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("world", world.getName());
            out.put("changed", changed);
            return out;
        });
    }

    private BlockData parseBlock(String block) {
        if (block == null || block.isBlank()) {
            throw new IllegalArgumentException("Missing block");
        }
        String raw = block.trim();
        try {
            return Bukkit.createBlockData(raw);
        } catch (IllegalArgumentException ex) {
            Material mat = Material.matchMaterial(raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw);
            if (mat == null || !mat.isBlock()) {
                throw new IllegalArgumentException("Invalid block: " + block);
            }
            return mat.createBlockData();
        }
    }

    private <T> T sync(Callable<T> task) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return task.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        Future<T> future = Bukkit.getScheduler().callSyncMethod(plugin, task);
        try {
            return future.get(120, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Timed out waiting for main thread", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for main thread", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(cause != null ? cause : e);
        }
    }
}
