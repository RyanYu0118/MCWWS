package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.restrictions.AxiomPermission;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

@Mixin(value = AxiomClient.class, remap = false)
public class AxiomClientSpectatorPermissionMixin {

    @Inject(method = "hasPermission", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mcwws$hasPermission(
            AxiomPermission permission,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (SurvivalEditorController.isServerSupported()
                && permission == AxiomPermission.PLAYER_GAMEMODE_SPECTATOR) {
            cir.setReturnValue(true);
        }
    }
}
