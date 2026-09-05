package work.mcwws.axiomsurvival;

import com.moulberry.axiom.packet.PacketHandler;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

final class PacketDelegate {

    private static final Method ON_RECEIVE = resolveOnReceive();

    private PacketDelegate() {
    }

    static void invoke(PacketHandler delegate, Player player, Object buf) {
        if (ON_RECEIVE == null) {
            McwwsAxiomSurvivalPlugin.getInstance().getLogger().severe(
                    "Axiom 包转发失败: 找不到 PacketHandler.onReceive");
            return;
        }
        try {
            ON_RECEIVE.invoke(delegate, player, buf);
        } catch (ReflectiveOperationException ex) {
            McwwsAxiomSurvivalPlugin.getInstance().getLogger().severe(
                    "Axiom 包转发失败: " + ex.getMessage());
        }
    }

    private static Method resolveOnReceive() {
        // AxiomPaper 6+: FriendlyByteBuf；5.x: RegistryFriendlyByteBuf（其子类）
        for (String bufName : new String[]{
                "net.minecraft.network.FriendlyByteBuf",
                "net.minecraft.network.RegistryFriendlyByteBuf"
        }) {
            try {
                Class<?> bufClass = Class.forName(bufName);
                return PacketHandler.class.getMethod("onReceive", Player.class, bufClass);
            } catch (ReflectiveOperationException ignored) {
                // try next signature
            }
        }
        return null;
    }
}
