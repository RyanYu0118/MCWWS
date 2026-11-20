import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.plugin.java.JavaPlugin;

public final class SurfaceRelight extends JavaPlugin {
    private static final int BATCH_SIZE = 64;
    private final Path plan = Path.of("surface-chunks.txt");
    private List<ChunkPos> positions;
    private CraftWorld world;
    private int cursor;

    @Override
    public void onEnable() {
        if (!Files.isRegularFile(plan)) {
            throw new IllegalStateException("This plugin only runs in a prepared snapshot server");
        }
        getServer().getScheduler().runTask(this, () -> {
            try {
                world = (CraftWorld) getServer().getWorld("world");
                getServer().getServerTickManager().setFrozen(true);
                positions = Files.readAllLines(plan).stream().map(line -> {
                    String[] coordinates = line.split(" ");
                    return new ChunkPos(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]));
                }).sorted(Comparator.comparingInt((ChunkPos position) -> Math.floorDiv(position.x(), 32))
                    .thenComparingInt(position -> Math.floorDiv(position.z(), 32))
                    .thenComparingInt(ChunkPos::z)
                    .thenComparingInt(ChunkPos::x)).toList();
                nextBatch();
            } catch (Exception failure) {
                fail(failure);
            }
        });
    }

    private void nextBatch() {
        try {
            if (cursor == positions.size()) {
                world.save();
                Files.writeString(Path.of("surface-relight.done"), Integer.toString(cursor));
                getServer().shutdown();
                return;
            }
            ChunkPos first = positions.get(cursor);
            int end = cursor + 1;
            while (end < positions.size() && end - cursor < BATCH_SIZE) {
                ChunkPos next = positions.get(end);
                if (Math.floorDiv(next.x(), 32) != Math.floorDiv(first.x(), 32)
                        || Math.floorDiv(next.z(), 32) != Math.floorDiv(first.z(), 32)) break;
                end++;
            }
            List<ChunkPos> batch = positions.subList(cursor, end);
            var loads = new ArrayList<CompletableFuture<Chunk>>(batch.size());
            for (ChunkPos position : batch) {
                loads.add(world.getChunkAtAsync(position.x(), position.z(), false));
            }
            CompletableFuture.allOf(loads.toArray(CompletableFuture<?>[]::new)).whenComplete((ignored, error) ->
                getServer().getScheduler().runTask(this, () -> {
                    try {
                        if (error != null) throw new IllegalStateException("Chunk load failed", error);
                        List<ChunkAccess> chunks = loads.stream().map(load ->
                            ((CraftChunk) load.join()).getHandle(ChunkStatus.FULL)).toList();
                        chunks.forEach(chunk -> chunk.setLightCorrect(true));
                        int queued = world.getHandle().getChunkSource().getLightEngine().starlight$serverRelightChunks(
                            batch, null, count -> getServer().getScheduler().runTask(this, () -> {
                                if (count != batch.size()) {
                                    fail(new IllegalStateException("Relit " + count + " of " + batch.size() + " chunks"));
                                    return;
                                }
                                chunks.forEach(ChunkAccess::markUnsaved);
                                cursor += count;
                                if (cursor % 4096 == 0 || cursor == positions.size()) {
                                    getLogger().info("Relit " + cursor + "/" + positions.size() + " chunks");
                                }
                                for (ChunkPos position : batch) {
                                    world.unloadChunkRequest(position.x(), position.z());
                                }
                                world.getHandle().getChunkSource().purgeUnload();
                                nextBatch();
                            }));
                        if (queued != batch.size()) throw new IllegalStateException("Relight skipped chunks");
                    } catch (Exception failure) {
                        fail(failure);
                    }
                }));
        } catch (Exception failure) {
            fail(failure);
        }
    }

    private void fail(Exception failure) {
        getLogger().log(java.util.logging.Level.SEVERE, "Snapshot relight failed", failure);
        getServer().shutdown();
    }
}
