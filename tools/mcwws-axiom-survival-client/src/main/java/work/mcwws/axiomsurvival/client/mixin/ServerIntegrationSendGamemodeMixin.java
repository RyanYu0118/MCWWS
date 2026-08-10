package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.integration.ServerIntegration;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

@Mixin(value = ServerIntegration.class, remap = false)
public class ServerIntegrationSendGamemodeMixin {

    @Inject(method = "sendChangeGameModeImmediately", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mcwws$sendChangeGameModeImmediately(GameType gameType, CallbackInfo ci) {
        if (SurvivalEditorController.shouldSuppressGamemodeSync()) {
            ci.cancel();
        }
    }

    @Inject(method = "sendPendingUpdates", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mcwws$sendPendingUpdates(CallbackInfo ci) {
        if (SurvivalEditorController.shouldSuppressGamemodeSync()) {
            ci.cancel();
        }
    }
}
