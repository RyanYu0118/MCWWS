package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.AxiomClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

@Mixin(value = AxiomClient.class, remap = false)
public class AxiomClientSpectatorPermissionMixin {

    private static final String SPECTATOR_PERMISSION = "axiom.player.gamemode.spectator";

    @Inject(method = "hasPermission", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mcwws$hasPermission(String permission, CallbackInfoReturnable<Boolean> cir) {
        if (SurvivalEditorController.isServerSupported() && SPECTATOR_PERMISSION.equals(permission)) {
            cir.setReturnValue(true);
        }
    }
}
