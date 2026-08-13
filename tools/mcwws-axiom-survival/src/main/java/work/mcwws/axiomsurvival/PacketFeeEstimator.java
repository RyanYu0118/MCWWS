package work.mcwws.axiomsurvival;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

final class PacketFeeEstimator {

    private final McwwsAxiomSurvivalPlugin plugin;
    private final Set<String> loggedFailures = ConcurrentHashMap.newKeySet();

    PacketFeeEstimator(McwwsAxiomSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    FeeAccumulator.Result estimateSetBlockPacket(Player player, Object friendlyByteBuf) {
        FeeAccumulator.Builder builder = newBuilder(player);
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
            logEstimateFailure("set_block", ex);
        } finally {
            setReaderIndex(friendlyByteBuf, mark);
        }
        return builder.build();
    }

    /** set_buffer 首字节 type：0=方块缓冲，1=生物群系缓冲 */
    record BufferEstimate(int type, FeeAccumulator.Result blocks, long biomeCells, double biomeMinDistance) {
    }

    BufferEstimate estimateSetBufferPacket(Player player, Object friendlyByteBuf) {
        FeeAccumulator.Builder builder = newBuilder(player);
        World world = player.getWorld();
        int mark;
        try {
            mark = readerIndex(friendlyByteBuf);
        } catch (ReflectiveOperationException ex) {
            return new BufferEstimate(0, builder.build(), 0L, FeeAccumulator.UNKNOWN_DISTANCE);
        }
        int type = 0;
        long biomeCells = 0L;
        double biomeMinDistance = FeeAccumulator.UNKNOWN_DISTANCE;
        try {
            readResourceKey(friendlyByteBuf);
            readUuid(friendlyByteBuf);
            type = readByte(friendlyByteBuf);
            if (type == 0) {
                Object registry = getBlockRegistry(player);
                Object buffer = loadBlockBuffer(friendlyByteBuf, registry, player);
                scanBlockBuffer(builder, world, buffer);
            } else if (type == 1) {
                BiomeScan scan = countBiomeCells(player, friendlyByteBuf);
                biomeCells = scan.cells();
                biomeMinDistance = scan.minDistance();
            }
        } catch (ReflectiveOperationException ex) {
            logEstimateFailure("set_buffer", ex);
        } finally {
            setReaderIndex(friendlyByteBuf, mark);
        }
        return new BufferEstimate(type, builder.build(), biomeCells, biomeMinDistance);
    }

    /** 实体类包（spawn/delete/manipulate）都以 readCollection 的 VarInt 数量开头 */
    long countCollection(Object friendlyByteBuf) {
        int mark;
        try {
            mark = readerIndex(friendlyByteBuf);
        } catch (ReflectiveOperationException ex) {
            return 0L;
        }
        try {
            int count = (int) friendlyByteBuf.getClass().getMethod("readVarInt").invoke(friendlyByteBuf);
            return Math.max(count, 0);
        } catch (ReflectiveOperationException ex) {
            logEstimateFailure("entity_count", ex);
            return 0L;
        } finally {
            setReaderIndex(friendlyByteBuf, mark);
        }
    }

    /**
     * 删除/调整实体包是 UUID 列表，按世界上已有实体的坐标算离玩家最近距离。
     * 生成包没有现成实体，采不到就返回未知。
     */
    double minDistanceFromEntityUuids(Player player, Object friendlyByteBuf) {
        int mark;
        try {
            mark = readerIndex(friendlyByteBuf);
        } catch (ReflectiveOperationException ex) {
            return FeeAccumulator.UNKNOWN_DISTANCE;
        }
        double nearest = FeeAccumulator.UNKNOWN_DISTANCE;
        try {
            int count = (int) friendlyByteBuf.getClass().getMethod("readVarInt").invoke(friendlyByteBuf);
            java.lang.reflect.Method readUuid = friendlyByteBuf.getClass().getMethod("readUUID");
            double ox = player.getLocation().getX();
            double oy = player.getLocation().getY();
            double oz = player.getLocation().getZ();
            World world = player.getWorld();
            for (int i = 0; i < count; i++) {
                Object uuid = readUuid.invoke(friendlyByteBuf);
                if (!(uuid instanceof java.util.UUID id)) {
                    break;
                }
                org.bukkit.entity.Entity entity = world.getEntity(id);
                if (entity == null) {
                    continue;
                }
                var location = entity.getLocation();
                double dx = location.getX() - ox;
                double dy = location.getY() - oy;
                double dz = location.getZ() - oz;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance < nearest) {
                    nearest = distance;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return FeeAccumulator.UNKNOWN_DISTANCE;
        } finally {
            setReaderIndex(friendlyByteBuf, mark);
        }
        return nearest;
    }

    private record BiomeScan(long cells, double minDistance) {
    }

    /** 统计生物群系缓冲里实际被涂改的格数（forEachEntry 会跳过默认值），并记下离玩家最近的一格 */
    private BiomeScan countBiomeCells(Player player, Object buf) throws ReflectiveOperationException {
        Class<?> biomeBufferClass = Class.forName("com.moulberry.axiom.buffer.BiomeBuffer");
        Object buffer = null;
        for (Method method : biomeBufferClass.getMethods()) {
            if ("load".equals(method.getName()) && method.getParameterCount() == 1) {
                buffer = method.invoke(null, buf);
                break;
            }
        }
        if (buffer == null) {
            return new BiomeScan(0L, FeeAccumulator.UNKNOWN_DISTANCE);
        }
        Class<?> consumerClass = Class.forName("com.moulberry.axiom.buffer.PositionConsumer");
        long[] count = {0L};
        double[] nearest = {FeeAccumulator.UNKNOWN_DISTANCE};
        double ox = player.getLocation().getX();
        double oy = player.getLocation().getY();
        double oz = player.getLocation().getZ();
        Object counter = java.lang.reflect.Proxy.newProxyInstance(
                consumerClass.getClassLoader(),
                new Class<?>[]{consumerClass},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("accept".equals(name)) {
                        count[0]++;
                        if (args != null && args.length >= 3
                                && args[0] instanceof Integer x
                                && args[1] instanceof Integer y
                                && args[2] instanceof Integer z) {
                            double dx = (x + 0.5D) - ox;
                            double dy = (y + 0.5D) - oy;
                            double dz = (z + 0.5D) - oz;
                            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (distance < nearest[0]) {
                                nearest[0] = distance;
                            }
                        }
                        return null;
                    }
                    if ("hashCode".equals(name)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(name)) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(name)) {
                        return "BiomeCellCounter";
                    }
                    return null;
                }
        );
        biomeBufferClass.getMethod("forEachEntry", consumerClass).invoke(buffer, counter);
        return new BiomeScan(count[0], nearest[0]);
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
            // fastutil 的 entry 实现是私有内部类，反射 getLongKey 会被 IllegalAccessException 挡下，
            // 只能走公共接口 Map.Entry 取键
            if (!(sectionEntry instanceof Map.Entry<?, ?> entry)
                    || !(entry.getKey() instanceof Number packed)) {
                continue;
            }
            long sectionKey = packed.longValue();
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
                            builder.addProtected(block);
                            continue;
                        }
                        BlockData target = NmsBlocks.toBlockData(newState);
                        builder.addChange(block, target);
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
            builder.addProtected(block);
            return;
        }
        BlockData target = NmsBlocks.toBlockData(newState);
        builder.addChange(block, target);
    }

    /**
     * 预估失败意味着这一包按 0 格放行、也就是免费建造，必须让管理员看见；
     * 但同一次编辑会拆成很多包，所以每个通道只在第一次抬到 WARNING。
     */
    private void logEstimateFailure(String channel, ReflectiveOperationException ex) {
        if (loggedFailures.add(channel)) {
            plugin.getLogger().log(Level.WARNING,
                    channel + " 扣费预估失败，这类改块将不计费，请检查 AxiomPaper 版本兼容性", ex);
        } else {
            plugin.getLogger().fine(channel + " 预估失败: " + ex.getMessage());
        }
    }

    private FeeAccumulator.Builder newBuilder(Player player) {
        if (plugin.reloadPricesBeforeEstimate()) {
            plugin.getPriceCatalog().reload();
        } else {
            plugin.getPriceCatalog().reloadIfStale();
        }
        int protectedCap = plugin.getPluginConfig().getInt("protection.max-restore-blocks", 4096);
        return new FeeAccumulator.Builder(
                plugin.getPriceCatalog(),
                plugin.laborRates(),
                plugin.salvageRate(),
                plugin.netMoves(),
                ProtectedBlockGuard.enabled() ? protectedCap : 0
        ).origin(player);
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
