package work.mcwws.axiomsurvival;

import com.moulberry.axiom.packet.PacketHandler;
import com.moulberry.axiom.packet.WrapperPacketListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class AxiomPaperHook {

    private final McwwsAxiomSurvivalPlugin plugin;
    private final ChargeService chargeService;
    private final PacketFeeEstimator estimator;
    private boolean installed;
    private final Set<PacketHandler> wrappedHandlers = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    public AxiomPaperHook(McwwsAxiomSurvivalPlugin plugin, ChargeService chargeService) {
        this.plugin = plugin;
        this.chargeService = chargeService;
        this.estimator = new PacketFeeEstimator(plugin);
    }

    public boolean install() {
        if (installed) {
            return true;
        }
        Plugin axiom = plugin.getServer().getPluginManager().getPlugin("AxiomPaper");
        if (axiom == null) {
            return false;
        }
        try {
            wrapChannel(axiom, "set_block");
            wrapChannel(axiom, "set_buffer");
            installed = true;
            plugin.getLogger().info("已挂钩 AxiomPaper set_block / set_buffer 扣费拦截。");
            return true;
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().log(Level.WARNING, "AxiomPaper 钩子安装失败", ex);
            return false;
        }
    }

    public static boolean isAxiomSessionActive(Player player) {
        if (player == null) {
            return false;
        }
        Plugin axiom = McwwsAxiomSurvivalPlugin.getInstance().getServer().getPluginManager().getPlugin("AxiomPaper");
        if (axiom == null) {
            return false;
        }
        try {
            Field field = axiom.getClass().getDeclaredField("activeAxiomPlayers");
            field.setAccessible(true);
            Object value = field.get(axiom);
            if (value instanceof Set<?> set) {
                return set.contains(player.getUniqueId());
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    private void wrapChannel(Plugin axiomPlugin, String channel) throws ReflectiveOperationException {
        Object identifier = resolveIdentifier("axiom", channel);
        PacketHandler original = lookupHandler(axiomPlugin, identifier);
        if (original == null) {
            throw new IllegalStateException("未找到 Axiom 包处理器: " + channel);
        }
        if (wrappedHandlers.contains(original)) {
            return;
        }

        PacketHandler wrapped = ChargingPacketHandlers.wrap(plugin, chargeService, estimator, original, channel);
        wrappedHandlers.add(wrapped);
        replaceHandler(axiomPlugin, identifier, wrapped);

        String channelName = "axiom:" + channel;
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(axiomPlugin, channelName);
        PluginMessageListener listener = new WrapperPacketListener(wrapped);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(axiomPlugin, channelName, listener);
    }

    @SuppressWarnings("unchecked")
    private PacketHandler lookupHandler(Plugin axiomPlugin, Object identifier) throws ReflectiveOperationException {
        Field field = axiomPlugin.getClass().getDeclaredField("supportedServerboundPackets");
        field.setAccessible(true);
        Map<Object, PacketHandler> map = (Map<Object, PacketHandler>) field.get(axiomPlugin);
        return map.get(identifier);
    }

    @SuppressWarnings("unchecked")
    private void replaceHandler(Plugin axiomPlugin, Object identifier, PacketHandler handler) throws ReflectiveOperationException {
        Field field = axiomPlugin.getClass().getDeclaredField("supportedServerboundPackets");
        field.setAccessible(true);
        Map<Object, PacketHandler> map = (Map<Object, PacketHandler>) field.get(axiomPlugin);
        map.put(identifier, handler);
    }

    private static Object resolveIdentifier(String namespace, String path) throws ReflectiveOperationException {
        Class<?> identifierClass = Class.forName("net.minecraft.resources.Identifier");
        Method method = identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class);
        return method.invoke(null, namespace, path);
    }
}
