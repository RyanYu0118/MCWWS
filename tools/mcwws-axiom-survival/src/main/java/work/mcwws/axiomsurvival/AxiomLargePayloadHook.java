package work.mcwws.axiomsurvival;

import com.moulberry.axiom.packet.PacketHandler;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 取得 Axiom 大载荷通道的处理器表。
 *
 * <p>AxiomPaper 对每个包按 {@code LargePayloadBehaviour} 分流：小载荷走 Bukkit 插件通道，
 * 大载荷则在 Bukkit 通道上只挂一个 {@code DummyPacketListener}，真正的处理器放进一张
 * {@code Map<String, PacketHandler>}，由 Netty 层的 {@code AxiomBigPayloadHandler} 按通道名查表调用。
 * {@code set_buffer}（Axiom 绝大多数改块都走它）是 {@code FORCE_LARGE}，所以只换 Bukkit 通道监听器
 * 根本拦不到，必须把这张表里的处理器一并包上扣费逻辑。
 *
 * <p>这张表由 AxiomPaper 在 onEnable 里创建，捕获在注册给 Paper 的 {@code ChannelInitializeListener}
 * 里，并且每条连接共用同一个实例；{@code AxiomBigPayloadHandler} 每收到一个包都重新 get 一次，
 * 因此替换表内条目对已在线和后续连接都立即生效。
 */
final class AxiomLargePayloadHook {

    private static final String HOLDER_CLASS = "io.papermc.paper.network.ChannelInitializeListenerHolder";
    private static final String AXIOM_PACKAGE = "com.moulberry.axiom";

    private AxiomLargePayloadHook() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, PacketHandler> findHandlers() throws ReflectiveOperationException {
        Class<?> holder = Class.forName(HOLDER_CLASS);
        Object listeners = holder.getMethod("getListeners").invoke(null);
        if (!(listeners instanceof Map<?, ?> map)) {
            return null;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object listener = entry.getValue();
            if (listener == null || !listener.getClass().getName().startsWith(AXIOM_PACKAGE)) {
                continue;
            }
            Map<String, PacketHandler> handlers = (Map<String, PacketHandler>) findMapField(listener);
            if (handlers != null) {
                return handlers;
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
