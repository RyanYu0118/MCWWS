package work.mcwws.axiomsurvival;

import com.moulberry.axiom.packet.PacketHandler;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class EditorPacketHandlers {

    private EditorPacketHandlers() {
    }

    static PacketHandler wrapGamemode(McwwsAxiomSurvivalPlugin plugin, EditorRestoreService restoreService, PacketHandler delegate) {
        return wrap(plugin, restoreService, delegate, EditorPacketHandlers::beforeGamemode, EditorPacketHandlers::afterGamemode);
    }

    static PacketHandler wrapTeleport(McwwsAxiomSurvivalPlugin plugin, EditorRestoreService restoreService, PacketHandler delegate) {
        return wrap(plugin, restoreService, delegate, EditorPacketHandlers::beforeTeleport, EditorPacketHandlers::afterTeleport);
    }

    @FunctionalInterface
    private interface BeforeAction {
        boolean apply(McwwsAxiomSurvivalPlugin plugin, EditorRestoreService restoreService, Player player, Object buf)
                throws ReflectiveOperationException;
    }

    @FunctionalInterface
    private interface AfterAction {
        void apply(EditorRestoreService restoreService, Player player, boolean restoreAfter);
    }

    private static PacketHandler wrap(
            McwwsAxiomSurvivalPlugin plugin,
            EditorRestoreService restoreService,
            PacketHandler delegate,
            BeforeAction before,
            AfterAction after
    ) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("handleAsync".equals(method.getName())) {
                return delegate.handleAsync();
            }
            if ("onReceive".equals(method.getName()) && args != null && args.length == 2) {
                Player player = (Player) args[0];
                Object buf = args[1];
                boolean restoreAfter = false;
                try {
                    restoreAfter = before.apply(plugin, restoreService, player, buf);
                } catch (ReflectiveOperationException ex) {
                    plugin.getLogger().fine("Editor 包预处理失败: " + ex.getMessage());
                }
                invokeDelegate(delegate, player, buf);
                after.apply(restoreService, player, restoreAfter);
                return null;
            }
            return method.invoke(delegate, args);
        };
        return (PacketHandler) Proxy.newProxyInstance(
                PacketHandler.class.getClassLoader(),
                new Class<?>[]{PacketHandler.class},
                handler
        );
    }

    private static boolean beforeGamemode(
            McwwsAxiomSurvivalPlugin plugin,
            EditorRestoreService restoreService,
            Player player,
            Object buf
    ) throws ReflectiveOperationException {
        if (!restoreService.enabled() || !player.hasPermission("mcwws.axiom.survival.use")) {
            return false;
        }
        int mark = PacketBufs.readerIndex(buf);
        int modeId = readByte(buf);
        PacketBufs.readerIndex(buf, mark);

        GameMode requested = GameMode.getByValue(modeId);
        if (requested == null) {
            return false;
        }
        if (requested == GameMode.SPECTATOR) {
            restoreService.onEnterSpectator(player);
            return false;
        }
        if (!EditorSessionState.has(player)) {
            return false;
        }
        if (requested == GameMode.CREATIVE && plugin.getPluginConfig().getBoolean("block-axiom-creative-switch", true)) {
            PacketBufs.readerIndex(buf, mark);
            writeByte(buf, GameMode.SURVIVAL.getValue());
            return true;
        }
        return requested == GameMode.SURVIVAL || requested == GameMode.ADVENTURE;
    }

    private static void afterGamemode(EditorRestoreService restoreService, Player player, boolean restoreAfter) {
        if (restoreAfter) {
            restoreService.scheduleRestore(player, 1L);
        }
    }

    private static boolean beforeTeleport(
            McwwsAxiomSurvivalPlugin plugin,
            EditorRestoreService restoreService,
            Player player,
            Object buf
    ) {
        if (!restoreService.enabled() || !EditorSessionState.has(player)) {
            return false;
        }
        return player.getGameMode() == GameMode.SPECTATOR;
    }

    private static void afterTeleport(EditorRestoreService restoreService, Player player, boolean restoreAfter) {
        if (restoreAfter) {
            restoreService.scheduleRestore(player, 2L);
        }
    }

    private static int readByte(Object buf) throws ReflectiveOperationException {
        return (byte) buf.getClass().getMethod("readByte").invoke(buf);
    }

    private static void writeByte(Object buf, int value) throws ReflectiveOperationException {
        buf.getClass().getMethod("writeByte", int.class).invoke(buf, value);
    }

    private static void invokeDelegate(PacketHandler delegate, Player player, Object buf) {
        PacketDelegate.invoke(delegate, player, buf);
    }
}
