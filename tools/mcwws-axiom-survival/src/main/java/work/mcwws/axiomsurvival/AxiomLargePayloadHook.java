package work.mcwws.axiomsurvival;

import com.moulberry.axiom.AxiomPaper;
import com.moulberry.axiom.packet.PacketHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 取得 Axiom 服务端「真正执行包」的处理器表。
 *
 * <p><b>AxiomPaper 6+</b>：大包改走 {@code axiom:tunnel}，解包后按
 * {@code supportedServerboundPackets}（{@code Map<Identifier, PacketHandler>}）查表调用。
 * 只换 Bukkit 插件通道拦不到隧道流量。
 *
 * <p><b>AxiomPaper 5.x</b>：按 {@code LargePayloadBehaviour} 分流，大载荷处理器挂在
 * {@code ChannelInitializeListener} 捕获的 {@code Map<String, PacketHandler>} 上，
 * 由 {@code AxiomBigPayloadHandler} 查表。
 */
final class AxiomLargePayloadHook {

    private static final String HOLDER_CLASS = "io.papermc.paper.network.ChannelInitializeListenerHolder";
    private static final String AXIOM_PACKAGE = "com.moulberry.axiom";

    private AxiomLargePayloadHook() {
    }

    /**
     * 可按 {@code axiom:channel} 读写的活表视图；写入会立刻影响已在线与后续连接。
     */
    interface HandlerTable {
        PacketHandler get(String channelName);

        void put(String channelName, PacketHandler handler);

        boolean contains(String channelName);
    }

    static HandlerTable findHandlers(AxiomPaper axiomPaper) throws ReflectiveOperationException {
        HandlerTable v6 = findSupportedServerbound(axiomPaper);
        if (v6 != null) {
            return v6;
        }
        return findLegacyLargePayloadTable();
    }

    @SuppressWarnings("unchecked")
    private static HandlerTable findSupportedServerbound(AxiomPaper axiomPaper)
            throws ReflectiveOperationException {
        Field field = findDeclaredField(axiomPaper.getClass(), "supportedServerboundPackets");
        if (field == null) {
            return null;
        }
        field.setAccessible(true);
        Object raw = field.get(axiomPaper);
        if (!(raw instanceof Map<?, ?> map) || map.isEmpty()) {
            return null;
        }
        Object sampleKey = map.keySet().iterator().next();
        if (sampleKey instanceof String) {
            Map<String, PacketHandler> stringMap = (Map<String, PacketHandler>) raw;
            return new HandlerTable() {
                @Override
                public PacketHandler get(String channelName) {
                    return stringMap.get(channelName);
                }

                @Override
                public void put(String channelName, PacketHandler handler) {
                    stringMap.put(channelName, handler);
                }

                @Override
                public boolean contains(String channelName) {
                    return stringMap.containsKey(channelName);
                }
            };
        }
        Class<?> identifierClass = Class.forName("net.minecraft.resources.Identifier");
        Method fromNamespaceAndPath = identifierClass.getMethod(
                "fromNamespaceAndPath", String.class, String.class);
        Map<Object, PacketHandler> idMap = (Map<Object, PacketHandler>) raw;
        return new HandlerTable() {
            private Object key(String channelName) {
                String path = channelName.startsWith("axiom:")
                        ? channelName.substring("axiom:".length())
                        : channelName;
                try {
                    return fromNamespaceAndPath.invoke(null, "axiom", path);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException(ex);
                }
            }

            @Override
            public PacketHandler get(String channelName) {
                return idMap.get(key(channelName));
            }

            @Override
            public void put(String channelName, PacketHandler handler) {
                idMap.put(key(channelName), handler);
            }

            @Override
            public boolean contains(String channelName) {
                return idMap.containsKey(key(channelName));
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static HandlerTable findLegacyLargePayloadTable() throws ReflectiveOperationException {
        Class<?> holder;
        try {
            holder = Class.forName(HOLDER_CLASS);
        } catch (ClassNotFoundException ex) {
            return null;
        }
        Object listeners = holder.getMethod("getListeners").invoke(null);
        if (!(listeners instanceof Map<?, ?> map)) {
            return null;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object listener = entry.getValue();
            if (listener == null || !listener.getClass().getName().startsWith(AXIOM_PACKAGE)) {
                continue;
            }
            Map<?, ?> handlers = findMapField(listener);
            if (handlers == null || handlers.isEmpty()) {
                continue;
            }
            Object sampleKey = handlers.keySet().iterator().next();
            if (!(sampleKey instanceof String)) {
                continue;
            }
            Map<String, PacketHandler> stringMap = (Map<String, PacketHandler>) handlers;
            return new HandlerTable() {
                @Override
                public PacketHandler get(String channelName) {
                    return stringMap.get(channelName);
                }

                @Override
                public void put(String channelName, PacketHandler handler) {
                    stringMap.put(channelName, handler);
                }

                @Override
                public boolean contains(String channelName) {
                    return stringMap.containsKey(channelName);
                }
            };
        }
        return null;
    }

    private static Field findDeclaredField(Class<?> type, String name) {
        Class<?> cursor = type;
        while (cursor != null && cursor != Object.class) {
            try {
                return cursor.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }

    private static Map<?, ?> findMapField(Object listener) throws ReflectiveOperationException {
        for (Field field : listener.getClass().getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            if (field.get(listener) instanceof Map<?, ?> value) {
                return value;
            }
        }
        return null;
    }
}
