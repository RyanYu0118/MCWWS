package work.mcwws.buildbridge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
    private final Deque<HistoryEntry> undoStack = new ArrayDeque<>();
    private final Deque<HistoryEntry> redoStack = new ArrayDeque<>();

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
            List<BlockSnap> before = new ArrayList<>(1);
            before.add(snap(world, x, y, z));
            world.getBlockAt(x, y, z).setBlockData(data, false);
            pushUndo(new HistoryEntry("set_block", world.getName(), before));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("world", world.getName());
            out.put("x", x);
            out.put("y", y);
            out.put("z", z);
            out.put("block", data.getAsString());
            out.put("undoable", true);
            out.put("undo_depth", undoStack.size());
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
            ensureHistoryCapacity((int) volume);
            BlockData data = parseBlock(block);
            List<BlockSnap> before = new ArrayList<>((int) Math.min(volume, Integer.MAX_VALUE));
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        before.add(snap(world, x, y, z));
                    }
                }
            }
            int changed = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        world.getBlockAt(x, y, z).setBlockData(data, false);
                        changed++;
                    }
                }
            }
            pushUndo(new HistoryEntry("fill", world.getName(), before));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("world", world.getName());
            out.put("changed", changed);
            out.put("block", data.getAsString());
            out.put("from", List.of(minX, minY, minZ));
            out.put("to", List.of(maxX, maxY, maxZ));
            out.put("undoable", true);
            out.put("undo_depth", undoStack.size());
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
            ensureHistoryCapacity(blocks.size());
            // First original per coordinate wins (for undo).
            LinkedHashMap<String, BlockSnap> beforeMap = new LinkedHashMap<>();
            List<int[]> writes = new ArrayList<>(blocks.size());
            List<BlockData> writeData = new ArrayList<>(blocks.size());
            for (Object raw : blocks) {
                Map<String, Object> item = JsonUtil.asObject(raw);
                int x = JsonUtil.i(item, "x");
                int y = JsonUtil.i(item, "y");
                int z = JsonUtil.i(item, "z");
                String block = JsonUtil.str(item, "block");
                if (block == null || block.isBlank()) {
                    throw new IllegalArgumentException("Each block entry needs block");
                }
                String k = x + "," + y + "," + z;
                beforeMap.putIfAbsent(k, snap(world, x, y, z));
                writes.add(new int[]{x, y, z});
                writeData.add(parseBlock(block));
            }
            for (int i = 0; i < writes.size(); i++) {
                int[] p = writes.get(i);
                world.getBlockAt(p[0], p[1], p[2]).setBlockData(writeData.get(i), false);
            }
            pushUndo(new HistoryEntry("set_blocks", world.getName(), new ArrayList<>(beforeMap.values())));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("world", world.getName());
            out.put("changed", writes.size());
            out.put("undoable", true);
            out.put("undo_depth", undoStack.size());
            return out;
        });
    }

    public Map<String, Object> undo(int times) {
        return sync(() -> {
            int n = Math.max(1, times);
            int restoredOps = 0;
            int restoredBlocks = 0;
            for (int i = 0; i < n; i++) {
                HistoryEntry entry = undoStack.pollFirst();
                if (entry == null) {
                    break;
                }
                World world = players.resolveWorld(entry.world);
                List<BlockSnap> after = new ArrayList<>(entry.before.size());
                for (BlockSnap s : entry.before) {
                    after.add(snap(world, s.x, s.y, s.z));
                }
                restoredBlocks += applySnaps(world, entry.before);
                redoStack.addFirst(new HistoryEntry(entry.op, entry.world, after));
                restoredOps++;
            }
            if (restoredOps == 0) {
                throw new IllegalStateException("Nothing to undo");
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("undone_operations", restoredOps);
            out.put("restored_blocks", restoredBlocks);
            out.put("undo_depth", undoStack.size());
            out.put("redo_depth", redoStack.size());
            return out;
        });
    }

    public Map<String, Object> redo(int times) {
        return sync(() -> {
            int n = Math.max(1, times);
            int restoredOps = 0;
            int restoredBlocks = 0;
            for (int i = 0; i < n; i++) {
                HistoryEntry entry = redoStack.pollFirst();
                if (entry == null) {
                    break;
                }
                World world = players.resolveWorld(entry.world);
                List<BlockSnap> before = new ArrayList<>(entry.before.size());
                for (BlockSnap s : entry.before) {
                    before.add(snap(world, s.x, s.y, s.z));
                }
                restoredBlocks += applySnaps(world, entry.before);
                undoStack.addFirst(new HistoryEntry(entry.op, entry.world, before));
                restoredOps++;
                trimUndo();
            }
            if (restoredOps == 0) {
                throw new IllegalStateException("Nothing to redo");
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("redone_operations", restoredOps);
            out.put("restored_blocks", restoredBlocks);
            out.put("undo_depth", undoStack.size());
            out.put("redo_depth", redoStack.size());
            return out;
        });
    }

    public Map<String, Object> historyStatus() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("enabled", historyEnabled());
        out.put("undo_depth", undoStack.size());
        out.put("redo_depth", redoStack.size());
        out.put("max_entries", maxEntries());
        out.put("max_blocks_per_entry", maxBlocksPerEntry());
        List<Map<String, Object>> recent = new ArrayList<>();
        int i = 0;
        for (HistoryEntry e : undoStack) {
            if (i++ >= 10) {
                break;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("op", e.op);
            row.put("world", e.world);
            row.put("blocks", e.before.size());
            recent.add(row);
        }
        out.put("recent_undo", recent);
        return out;
    }

    private int applySnaps(World world, List<BlockSnap> snaps) {
        for (BlockSnap snap : snaps) {
            world.getBlockAt(snap.x, snap.y, snap.z).setBlockData(Bukkit.createBlockData(snap.block), false);
        }
        return snaps.size();
    }

    private void pushUndo(HistoryEntry entry) {
        if (!historyEnabled()) {
            return;
        }
        undoStack.addFirst(entry);
        redoStack.clear();
        trimUndo();
    }

    private void trimUndo() {
        int max = maxEntries();
        while (undoStack.size() > max) {
            undoStack.removeLast();
        }
    }

    private void ensureHistoryCapacity(int blocks) {
        if (!historyEnabled()) {
            return;
        }
        int maxBlocks = maxBlocksPerEntry();
        if (blocks > maxBlocks) {
            throw new IllegalArgumentException(
                    "Operation touches " + blocks + " blocks; exceeds history.max-blocks-per-entry="
                            + maxBlocks + " (split the write or raise the limit)"
            );
        }
    }

    private boolean historyEnabled() {
        return plugin.getConfig().getBoolean("history.enabled", true);
    }

    private int maxEntries() {
        return Math.max(1, plugin.getConfig().getInt("history.max-entries", 20));
    }

    private int maxBlocksPerEntry() {
        return Math.max(1, plugin.getConfig().getInt("history.max-blocks-per-entry", 200000));
    }

    private static BlockSnap snap(World world, int x, int y, int z) {
        return new BlockSnap(x, y, z, world.getBlockAt(x, y, z).getBlockData().getAsString());
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

    private static final class BlockSnap {
        final int x;
        final int y;
        final int z;
        final String block;

        BlockSnap(int x, int y, int z, String block) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
        }
    }

    private static final class HistoryEntry {
        final String op;
        final String world;
        /** Snaps applied when this entry runs (undo: originals; redo: after-state). */
        final List<BlockSnap> before;

        HistoryEntry(String op, String world, List<BlockSnap> before) {
            this.op = op;
            this.world = world;
            this.before = before;
        }
    }
}
