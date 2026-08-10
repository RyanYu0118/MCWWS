package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.integration.ServerIntegration;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

@Mixin(value = ServerIntegration.class, remap = false)
public class ServerIntegrationChangeGameModeMixin {

    @Inject(method = "changeGameMode", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mcwws$changeGameMode(GameType gameType, CallbackInfo ci) {
        if (!SurvivalEditorController.isServerSupported()) {
            return;
        }
        if (gameType == GameType.SPECTATOR) {
            SurvivalEditorController.onEditorEnter();
            ci.cancel();
            return;
        }
        if (SurvivalEditorController.isLocalEditorActive()) {
            SurvivalEditorController.onEditorExit();
            ci.cancel();
        }
    }
}
