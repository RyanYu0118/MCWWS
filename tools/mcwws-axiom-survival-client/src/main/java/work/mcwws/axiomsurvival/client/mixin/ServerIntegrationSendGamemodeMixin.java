package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.integration.ServerIntegration;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

import java.util.List;

@Mixin(value = ServerIntegration.class, remap = false)
public class ServerIntegrationSendGamemodeMixin {

    @Shadow
    private static GameType pendingGameType;

    @Shadow
    private static Integer pendingTime;

    @Shadow
    private static Boolean pendingTimeFrozen;

    @Shadow
    private static List<Packet<?>> pendingPackets;

    @Inject(method = "sendChangeGameModeImmediately", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mcwws$sendChangeGameModeImmediately(GameType gameType, CallbackInfo ci) {
        if (SurvivalEditorController.shouldSuppressGamemodeSync()) {
            ci.cancel();
        }
    }

    /**
     * 生存 Editor 期间不能把创造/旁观模式包和时间包发到服务端，但飞行速度必须照发。
     * 以前整段 {@code sendPendingUpdates} 被取消，编辑界面里调的速度从未到达服务端。
     */
    @Inject(method = "sendPendingUpdates", at = @At("HEAD"), remap = false)
    private static void mcwws$keepFlySpeedSync(CallbackInfo ci) {
        if (!SurvivalEditorController.shouldSuppressGamemodeSync()) {
            return;
        }
        pendingGameType = null;
        pendingTime = null;
        pendingTimeFrozen = null;
        if (pendingPackets != null) {
            pendingPackets.clear();
        }
    }
}
