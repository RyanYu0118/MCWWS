package work.mcwws.buildbridge;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 控制台身份的 FAWE 立方填充。不走玩家指令，因此不触发生存创世神扣费。
 * 关闭历史，避免数十亿方块的撤销记录撑满磁盘。
 */
public final class FaweBulkService {

    static final int MAX_JOBS = 8000;
    static final long MAX_BLOCKS = 30_000_000L;
    private static final int PLAYER_PAD = 6;

    private final McwwsBuildBridgePlugin plugin;

    public FaweBulkService(McwwsBuildBridgePlugin plugin) {
        this.plugin = plugin;
    }

    public Map<String, Object> fill(String worldName, List<Object> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            throw new IllegalArgumentException("jobs required");
        }
        if (jobs.size() > MAX_JOBS) {
            throw new IllegalArgumentException("jobs " + jobs.size() + " exceeds " + MAX_JOBS);
        }
        World world = Bukkit.getWorld(worldName == null || worldName.isBlank() ? "world" : worldName);
        if (world == null) {
            throw new IllegalArgumentException("Unknown world");
        }
        List<int[]> boxes = new ArrayList<>(jobs.size());
        List<String> blocks = new ArrayList<>(jobs.size());
        long volume = 0L;
        for (Object raw : jobs) {
            Map<String, Object> item = JsonUtil.asObject(raw);
            int x1 = JsonUtil.i(item, "x1");
            int y1 = JsonUtil.i(item, "y1");
            int z1 = JsonUtil.i(item, "z1");
            int x2 = JsonUtil.i(item, "x2");
            int y2 = JsonUtil.i(item, "y2");
            int z2 = JsonUtil.i(item, "z2");
            String block = JsonUtil.str(item, "block");
            if (block == null || block.isBlank()) {
                throw new IllegalArgumentException("job missing block");
            }
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);
            if (minY < world.getMinHeight() || maxY >= world.getMaxHeight()) {
                throw new IllegalArgumentException(
                        "Y " + minY + ".." + maxY + " outside world height "
                                + world.getMinHeight() + ".." + (world.getMaxHeight() - 1));
            }
            long boxVolume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
            volume += boxVolume;
            if (volume > MAX_BLOCKS) {
                throw new IllegalArgumentException("batch volume " + volume + " exceeds " + MAX_BLOCKS);
            }
            boxes.add(new int[]{minX, minY, minZ, maxX, maxY, maxZ});
            blocks.add(block);
        }

        List<int[]> bubbles = playerBubbles(world);
        List<int[]> clippedBoxes = new ArrayList<>();
        List<String> clippedBlocks = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++) {
            List<int[]> parts = List.of(boxes.get(i));
            for (int[] bubble : bubbles) {
                List<int[]> next = new ArrayList<>();
                for (int[] part : parts) {
                    next.addAll(subtract(part, bubble));
                }
                parts = next;
            }
            for (int[] part : parts) {
                clippedBoxes.add(part);
                clippedBlocks.add(blocks.get(i));
            }
        }

        long changed = clippedBoxes.isEmpty() ? 0L : edit(world, clippedBoxes, clippedBlocks);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("world", world.getName());
        out.put("jobs", jobs.size());
        out.put("boxes", clippedBoxes.size());
        out.put("changed", changed);
        out.put("player_bubbles", bubbles.size());
        out.put("history", false);
        return out;
    }

    private long edit(World world, List<int[]> boxes, List<String> blocks) {
        Plugin fawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        if (fawe == null) {
            throw new IllegalStateException("FastAsyncWorldEdit is not loaded");
        }
        ClassLoader loader = fawe.getClass().getClassLoader();
        try {
            Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit", true, loader);
            Class<?> weWorldClass = Class.forName("com.sk89q.worldedit.world.World", true, loader);
            Class<?> adapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter", true, loader);
            Class<?> vectorClass = Class.forName("com.sk89q.worldedit.math.BlockVector3", true, loader);
            Class<?> regionClass = Class.forName("com.sk89q.worldedit.regions.CuboidRegion", true, loader);
            Class<?> regionIface = Class.forName("com.sk89q.worldedit.regions.Region", true, loader);
            Class<?> holderClass = Class.forName("com.sk89q.worldedit.world.block.BlockStateHolder", true, loader);
            Class<?> sideClass = Class.forName("com.sk89q.worldedit.util.SideEffectSet", true, loader);
            Class<?> relightClass = Class.forName(
                    "com.fastasyncworldedit.core.extent.processor.lighting.RelightMode", true, loader);
            Class<?> sessionClass = Class.forName("com.sk89q.worldedit.EditSession", true, loader);

            Object worldEdit = worldEditClass.getMethod("getInstance").invoke(null);
            Object builder = worldEditClass.getMethod("newEditSessionBuilder").invoke(worldEdit);
            Object weWorld = adapterClass.getMethod("adapt", World.class).invoke(null, world);
            Object console = adapterClass.getMethod("adapt", org.bukkit.command.CommandSender.class)
                    .invoke(null, Bukkit.getConsoleSender());
            Object sideNone = sideClass.getMethod("none").invoke(null);
            Object relight = relightClass.getMethod("valueOf", String.class).invoke(null, "OPTIMAL");

            builder = builder.getClass().getMethod("world", weWorldClass).invoke(builder, weWorld);
            builder = builder.getClass().getMethod("actor", Class.forName(
                    "com.sk89q.worldedit.extension.platform.Actor", true, loader)).invoke(builder, console);
            builder = builder.getClass().getMethod("changeSetNull").invoke(builder);
            builder = builder.getClass().getMethod("limitUnlimited").invoke(builder);
            builder = builder.getClass().getMethod("allowedRegionsEverywhere").invoke(builder);
            builder = builder.getClass().getMethod("fastMode", Boolean.class).invoke(builder, Boolean.TRUE);
            builder = builder.getClass().getMethod("checkMemory", Boolean.class).invoke(builder, Boolean.FALSE);
            builder = builder.getClass().getMethod("setSideEffectSet", sideClass).invoke(builder, sideNone);
            builder = builder.getClass().getMethod("relightMode", relightClass).invoke(builder, relight);
            builder = builder.getClass().getMethod("command", String.class).invoke(builder, "mcwws-fawe-bulk");

            Method at = vectorClass.getMethod("at", int.class, int.class, int.class);
            var regionCtor = regionClass.getConstructor(vectorClass, vectorClass);
            Method adaptBlock = adapterClass.getMethod("adapt", BlockData.class);
            Method setBlocks = findSetBlocks(sessionClass, regionIface, holderClass);
            Method close = sessionClass.getMethod("close");

            Object session = builder.getClass().getMethod("build").invoke(builder);
            long changed = 0L;
            try {
                for (int i = 0; i < boxes.size(); i++) {
                    int[] box = boxes.get(i);
                    BlockData data = Bukkit.createBlockData(blocks.get(i));
                    Object state = adaptBlock.invoke(null, data);
                    Object region = regionCtor.newInstance(
                            at.invoke(null, box[0], box[1], box[2]),
                            at.invoke(null, box[3], box[4], box[5]));
                    Object count = setBlocks.invoke(session, region, state);
                    if (count instanceof Integer n) {
                        changed += n;
                    }
                }
            } finally {
                close.invoke(session);
            }
            return changed;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new IllegalStateException("FAWE bulk failed: " + cause.getMessage(), cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("FAWE bulk failed: " + e.getMessage(), e);
        }
    }

    private static Method findSetBlocks(Class<?> sessionClass, Class<?> regionIface, Class<?> holderClass) {
        for (Method method : sessionClass.getMethods()) {
            if (!"setBlocks".equals(method.getName()) || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params[0].isAssignableFrom(regionIface) && params[1].isAssignableFrom(holderClass)) {
                return method;
            }
        }
        throw new IllegalStateException("EditSession.setBlocks(Region, BlockState) not found");
    }

    private List<int[]> playerBubbles(World world) {
        Callable<List<int[]>> task = () -> {
            List<int[]> out = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld() != world) {
                    continue;
                }
                Location loc = player.getLocation();
                int x = loc.getBlockX();
                int y = loc.getBlockY();
                int z = loc.getBlockZ();
                out.add(new int[]{x - PLAYER_PAD, y - PLAYER_PAD, z - PLAYER_PAD, x + PLAYER_PAD, y + 2 + PLAYER_PAD, z + PLAYER_PAD});
            }
            return out;
        };
        try {
            if (Bukkit.isPrimaryThread()) {
                return task.call();
            }
            return Bukkit.getScheduler().callSyncMethod(plugin, task).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read player positions", e);
        }
    }

    static List<int[]> subtract(int[] box, int[] cut) {
        int ix1 = Math.max(box[0], cut[0]);
        int iy1 = Math.max(box[1], cut[1]);
        int iz1 = Math.max(box[2], cut[2]);
        int ix2 = Math.min(box[3], cut[3]);
        int iy2 = Math.min(box[4], cut[4]);
        int iz2 = Math.min(box[5], cut[5]);
        if (ix1 > ix2 || iy1 > iy2 || iz1 > iz2) {
            return List.of(box);
        }
        List<int[]> parts = new ArrayList<>(6);
        if (box[0] < ix1) {
            parts.add(new int[]{box[0], box[1], box[2], ix1 - 1, box[4], box[5]});
        }
        if (ix2 < box[3]) {
            parts.add(new int[]{ix2 + 1, box[1], box[2], box[3], box[4], box[5]});
        }
        if (box[1] < iy1) {
            parts.add(new int[]{ix1, box[1], box[2], ix2, iy1 - 1, box[5]});
        }
        if (iy2 < box[4]) {
            parts.add(new int[]{ix1, iy2 + 1, box[2], ix2, box[4], box[5]});
        }
        if (box[2] < iz1) {
            parts.add(new int[]{ix1, iy1, box[2], ix2, iy2, iz1 - 1});
        }
        if (iz2 < box[5]) {
            parts.add(new int[]{ix1, iy1, iz2 + 1, ix2, iy2, box[5]});
        }
        return parts;
    }
}
