package work.mcwws.axiomsurvival;

import com.moulberry.axiom.packet.PacketHandler;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

final class EditorPacketHandlers {

    private enum BeforeResult {
        PROCEED,
        RESTORE_NOW,
        SKIP
    }

    private EditorPacketHandlers() {
    }

    static PacketHandler wrapGamemode(McwwsAxiomSurvivalPlugin plugin, EditorRestoreService restoreService, PacketHandler delegate) {
        return wrap(plugin, restoreService, delegate, EditorPacketHandlers::beforeGamemode);
    }

    static PacketHandler wrapTeleport(McwwsAxiomSurvivalPlugin plugin, EditorRestoreService restoreService, PacketHandler delegate) {
        return wrap(plugin, restoreService, delegate, EditorPacketHandlers::beforeTeleport);
    }

    @FunctionalInterface
    private interface BeforeAction {
        BeforeResult apply(McwwsAxiomSurvivalPlugin plugin, EditorRestoreService restoreService, Player player, Object buf)
                throws ReflectiveOperationException;
    }

    private static PacketHandler wrap(
            McwwsAxiomSurvivalPlugin plugin,
            EditorRestoreService restoreService,
            PacketHandler delegate,
            BeforeAction before
    ) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("handleAsync".equals(method.getName())) {
                return delegate.handleAsync();
            }
            if ("onReceive".equals(method.getName()) && args != null && args.length == 2) {
                Player player = (Player) args[0];
                Object buf = args[1];
                BeforeResult result = BeforeResult.PROCEED;
                try {
                    result = before.apply(plugin, restoreService, player, buf);
                } catch (ReflectiveOperationException ex) {
                    plugin.getLogger().fine("Editor 包预处理失败: " + ex.getMessage());
                }
                switch (result) {
                    case RESTORE_NOW -> restoreService.restoreNow(player);
                    case SKIP -> { }
                    case PROCEED -> {
                        PacketDelegate.invoke(delegate, player, buf);
                    }
                }
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

    private static BeforeResult beforeGamemode(
            McwwsAxiomSurvivalPlugin plugin,
            EditorRestoreService restoreService,
            Player player,
            Object buf
    ) throws ReflectiveOperationException {
        if (!restoreService.enabled() || !player.hasPermission("mcwws.axiom.survival.use")) {
            return BeforeResult.PROCEED;
        }
        int mark = PacketBufs.readerIndex(buf);
        int modeId = readByte(buf);
        PacketBufs.readerIndex(buf, mark);

        GameMode requested = GameMode.getByValue(modeId);
        if (requested == null) {
            return BeforeResult.PROCEED;
        }
        if (EditorSessionState.isInRestoreGrace(player)) {
            return BeforeResult.SKIP;
        }
        if (requested == GameMode.SPECTATOR) {
            restoreService.onEnterSpectator(player);
            return BeforeResult.PROCEED;
        }
        if (EditorSessionState.has(player)) {
            return BeforeResult.RESTORE_NOW;
        }
        if (requested == GameMode.CREATIVE
                && plugin.getPluginConfig().getBoolean("block-axiom-creative-switch", true)) {
            return BeforeResult.SKIP;
        }
        return BeforeResult.PROCEED;
    }

    private static BeforeResult beforeTeleport(
            McwwsAxiomSurvivalPlugin plugin,
            EditorRestoreService restoreService,
            Player player,
            Object buf
    ) {
        if (!restoreService.enabled() || !player.hasPermission("mcwws.axiom.survival.use")) {
            return BeforeResult.PROCEED;
        }
        if (EditorSessionState.isInRestoreGrace(player)) {
            return BeforeResult.SKIP;
        }
        return BeforeResult.PROCEED;
    }

    private static int readByte(Object buf) throws ReflectiveOperationException {
        return (byte) buf.getClass().getMethod("readByte").invoke(buf);
    }
}
