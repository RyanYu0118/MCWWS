package work.mcwws.axiomsurvival;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;

final class PacketFeeEstimator {

    private final McwwsAxiomSurvivalPlugin plugin;

    PacketFeeEstimator(McwwsAxiomSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    FeeAccumulator.Result estimateSetBlockPacket(Player player, Object friendlyByteBuf) {
        FeeAccumulator.Builder builder = newBuilder();
        World world = player.getWorld();
        int mark;
        try {
            mark = readerIndex(friendlyByteBuf);
        } catch (ReflectiveOperationException ex) {
            return builder.build();
        }
        try {
            Object registry = getBlockRegistry(player);
            Object blocks = readBlockMap(friendlyByteBuf, registry);
            if (blocks instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    accumulateEntry(builder, world, entry.getKey(), entry.getValue());
                }
            }
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().fine("set_block 预估失败: " + ex.getMessage());
        } finally {
            setReaderIndex(friendlyByteBuf, mark);
        }
        return builder.build();
    }

    FeeAccumulator.Result estimateSetBufferPacket(Player player, Object friendlyByteBuf) {
        FeeAccumulator.Builder builder = newBuilder();
        World world = player.getWorld();
        int mark;
        try {
            mark = readerIndex(friendlyByteBuf);
        } catch (ReflectiveOperationException ex) {
            return builder.build();
        }
        try {
            readResourceKey(friendlyByteBuf);
            readUuid(friendlyByteBuf);
            int type = readByte(friendlyByteBuf);
            if (type != 0) {
                return builder.build();
            }
            Object registry = getBlockRegistry(player);
            Object buffer = loadBlockBuffer(friendlyByteBuf, registry, player);
            scanBlockBuffer(builder, world, buffer);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().fine("set_buffer 预估失败: " + ex.getMessage());
        } finally {
            setReaderIndex(friendlyByteBuf, mark);
        }
        return builder.build();
    }

    private void scanBlockBuffer(FeeAccumulator.Builder builder, World world, Object blockBuffer) throws ReflectiveOperationException {
        Object entrySet = blockBuffer.getClass().getMethod("entrySet").invoke(blockBuffer);
        if (!(entrySet instanceof Iterable<?> sections)) {
            return;
        }
        Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
        Method getX = blockPosClass.getMethod("getX", long.class);
        Method getY = blockPosClass.getMethod("getY", long.class);
        Method getZ = blockPosClass.getMethod("getZ", long.class);
        Method bufferGet = blockBuffer.getClass().getMethod("get", int.class, int.class, int.class);

        for (Object sectionEntry : sections) {
            long sectionKey = (long) sectionEntry.getClass().getMethod("getLongKey").invoke(sectionEntry);
            int cx = (int) getX.invoke(null, sectionKey);
            int cy = (int) getY.invoke(null, sectionKey);
            int cz = (int) getZ.invoke(null, sectionKey);
            int baseX = cx << 4;
            int baseY = cy << 4;
            int baseZ = cz << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        Object newState = bufferGet.invoke(blockBuffer, baseX + x, baseY + y, baseZ + z);
                        if (newState == null) {
                            continue;
                        }
                        Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);
                        if (BlockProtection.isProtectedBlock(block)) {
                            builder.addProtected();
                            continue;
                        }
                        BlockData target = NmsBlocks.toBlockData(newState);
                        builder.addChange(block.getBlockData(), target);
                    }
                }
            }
        }
    }

    private void accumulateEntry(FeeAccumulator.Builder builder, World world, Object blockPos, Object newState) throws ReflectiveOperationException {
        int x = NmsBlocks.blockPosX(blockPos);
        int y = NmsBlocks.blockPosY(blockPos);
        int z = NmsBlocks.blockPosZ(blockPos);
        Block block = world.getBlockAt(x, y, z);
        if (BlockProtection.isProtectedBlock(block)) {
            builder.addProtected();
            return;
        }
        BlockData target = NmsBlocks.toBlockData(newState);
        builder.addChange(block.getBlockData(), target);
    }

    private FeeAccumulator.Builder newBuilder() {
        if (plugin.reloadPricesBeforeEstimate()) {
            plugin.getPriceCatalog().reload();
        } else {
            plugin.getPriceCatalog().reloadIfStale();
        }
        return new FeeAccumulator.Builder(plugin.getPriceCatalog(), plugin.laborRates());
    }

    private Object getBlockRegistry(Player player) throws ReflectiveOperationException {
        Object axiom = plugin.getServer().getPluginManager().getPlugin("AxiomPaper");
        return axiom.getClass().getMethod("getBlockRegistry", java.util.UUID.class).invoke(axiom, player.getUniqueId());
    }

    private Object readBlockMap(Object buf, Object registry) throws ReflectiveOperationException {
        Method readMap = findReadMapMethod(buf.getClass());
        java.util.function.IntFunction<java.util.Map<Object, Object>> mapFactory =
                size -> new java.util.LinkedHashMap<>(Math.max(size, 16));
        java.util.function.Function<Object, Object> keyReader = b -> {
            try {
                return invokeNoArg(b, "readBlockPos");
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        };
        Method byIdOrThrow = registry.getClass().getMethod("byIdOrThrow", int.class);
        java.util.function.Function<Integer, Object> idMapper = id -> {
            try {
                return byIdOrThrow.invoke(registry, id);
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        };
        java.util.function.Function<Object, Object> valReader = b -> {
            try {
                return invokeOneArg(b, "readById", idMapper);
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        };
        return readMap.invoke(buf, mapFactory, keyReader, valReader);
    }

    private Object loadBlockBuffer(Object buf, Object registry, Player player) throws ReflectiveOperationException {
        Class<?> bufferClass = Class.forName("com.moulberry.axiom.buffer.BlockBuffer");
        for (Method method : bufferClass.getMethods()) {
            if ("load".equals(method.getName()) && method.getParameterCount() == 3) {
                return method.invoke(null, buf, registry, player);
            }
        }
        throw new NoSuchMethodException("BlockBuffer.load");
    }

    private static Method findReadMapMethod(Class<?> type) throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if ("readMap".equals(method.getName()) && method.getParameterCount() == 3) {
                    method.setAccessible(true);
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException("readMap");
    }

    private static Object invokeNoArg(Object target, String methodName) throws ReflectiveOperationException {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static Object invokeOneArg(Object target, String methodName, Object arg) throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                return method.invoke(target, arg);
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private void readResourceKey(Object buf) throws ReflectiveOperationException {
        Class<?> registries = Class.forName("net.minecraft.core.registries.Registries");
        Object dimension = registries.getField("DIMENSION").get(null);
        buf.getClass().getMethod("readResourceKey", Class.forName("net.minecraft.resources.ResourceKey"))
                .invoke(buf, dimension);
    }

    private void readUuid(Object buf) throws ReflectiveOperationException {
        buf.getClass().getMethod("readUUID").invoke(buf);
    }

    private int readByte(Object buf) throws ReflectiveOperationException {
        return (byte) buf.getClass().getMethod("readByte").invoke(buf);
    }

    private int readerIndex(Object buf) throws ReflectiveOperationException {
        return PacketBufs.readerIndex(buf);
    }

    private void setReaderIndex(Object buf, int index) {
        try {
            PacketBufs.readerIndex(buf, index);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
