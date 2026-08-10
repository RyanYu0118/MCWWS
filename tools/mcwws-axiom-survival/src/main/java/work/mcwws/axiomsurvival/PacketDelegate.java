package work.mcwws.axiomsurvival;

import com.moulberry.axiom.packet.PacketHandler;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

final class PacketDelegate {

    private PacketDelegate() {
    }

    static void invoke(PacketHandler delegate, Player player, Object buf) {
        try {
            Class<?> bufClass = Class.forName("net.minecraft.network.RegistryFriendlyByteBuf");
            Method onReceive = PacketHandler.class.getMethod("onReceive", Player.class, bufClass);
            onReceive.invoke(delegate, player, buf);
        } catch (ReflectiveOperationException ex) {
            McwwsAxiomSurvivalPlugin.getInstance().getLogger().severe("Axiom 包转发失败: " + ex.getMessage());
        }
    }
}
