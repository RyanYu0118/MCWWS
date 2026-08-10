package work.mcwws.axiomsurvival;

import com.moulberry.axiom.packet.PacketHandler;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

final class EditorPacketHandlers {

    private enum BeforeResult {
        PROCEED,
        SKIP,
        SKIP_EDITOR_ENTER
    }

    private EditorPacketHandlers() {
    }

    static PacketHandler wrapGamemode(
            McwwsAxiomSurvivalPlugin plugin,
            EditorSurvivalService editorSurvivalService,
            PacketHandler delegate
    ) {
        return wrap(plugin, editorSurvivalService, delegate, EditorPacketHandlers::beforeGamemode);
    }

    static PacketHandler wrapNoPhysicalTrigger(
            McwwsAxiomSurvivalPlugin plugin,
            EditorSurvivalService editorSurvivalService,
            PacketHandler delegate
    ) {
        return wrap(plugin, editorSurvivalService, delegate, EditorPacketHandlers::beforeNoPhysicalTrigger);
    }

    @FunctionalInterface
    private interface BeforeAction {
        BeforeResult apply(
                McwwsAxiomSurvivalPlugin plugin,
                EditorSurvivalService editorSurvivalService,
                Player player,
                Object buf
        ) throws ReflectiveOperationException;
    }

    private static PacketHandler wrap(
            McwwsAxiomSurvivalPlugin plugin,
            EditorSurvivalService editorSurvivalService,
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
                    result = before.apply(plugin, editorSurvivalService, player, buf);
                } catch (ReflectiveOperationException ex) {
                    plugin.getLogger().fine("Editor 包预处理失败: " + ex.getMessage());
                }
                switch (result) {
                    case SKIP, SKIP_EDITOR_ENTER -> { }
                    case PROCEED -> PacketDelegate.invoke(delegate, player, buf);
                }
                if (result == BeforeResult.SKIP_EDITOR_ENTER) {
                    editorSurvivalService.onEditorEnter(player);
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
            EditorSurvivalService editorSurvivalService,
            Player player,
            Object buf
    ) throws ReflectiveOperationException {
        if (!player.hasPermission("mcwws.axiom.survival.use") || BlockProtection.shouldBypass(player)) {
            return BeforeResult.PROCEED;
        }
        int mark = PacketBufs.readerIndex(buf);
        int modeId = readByte(buf);
        PacketBufs.readerIndex(buf, mark);

        GameMode requested = GameMode.getByValue(modeId);
        if (requested == null) {
            return BeforeResult.PROCEED;
        }
        if (requested == GameMode.SPECTATOR && editorSurvivalService.enabled()) {
            return BeforeResult.SKIP_EDITOR_ENTER;
        }
        if (requested == GameMode.CREATIVE
                && plugin.getPluginConfig().getBoolean("block-axiom-creative-switch", true)) {
            return BeforeResult.SKIP;
        }
        return BeforeResult.PROCEED;
    }

    private static BeforeResult beforeNoPhysicalTrigger(
            McwwsAxiomSurvivalPlugin plugin,
            EditorSurvivalService editorSurvivalService,
            Player player,
            Object buf
    ) throws ReflectiveOperationException {
        if (!editorSurvivalService.enabled() || !player.hasPermission("mcwws.axiom.survival.use")) {
            return BeforeResult.PROCEED;
        }
        int mark = PacketBufs.readerIndex(buf);
        boolean enabled = readBoolean(buf);
        PacketBufs.readerIndex(buf, mark);
        if (enabled) {
            return BeforeResult.SKIP;
        }
        return BeforeResult.PROCEED;
    }

    private static int readByte(Object buf) throws ReflectiveOperationException {
        return (byte) buf.getClass().getMethod("readByte").invoke(buf);
    }

    private static boolean readBoolean(Object buf) throws ReflectiveOperationException {
        return (boolean) buf.getClass().getMethod("readBoolean").invoke(buf);
    }
}
