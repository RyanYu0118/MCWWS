package work.mcwws.axiomsurvival.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

/**
 * Editor 建造时显示生存 HUD（含经验条）；菜单打开时隐藏。
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeHudMixin {

    @Inject(method = "canHurtPlayer", at = @At("HEAD"), cancellable = true)
    private void mcwws$canHurtPlayer(CallbackInfoReturnable<Boolean> cir) {
        if (SurvivalEditorController.isLocalEditorActive()) {
            cir.setReturnValue(SurvivalEditorController.shouldShowSurvivalHud());
        }
    }

    @Inject(method = "hasExperience", at = @At("HEAD"), cancellable = true)
    private void mcwws$hasExperience(CallbackInfoReturnable<Boolean> cir) {
        if (SurvivalEditorController.isLocalEditorActive()) {
            cir.setReturnValue(SurvivalEditorController.shouldShowSurvivalHud());
        }
    }
}
