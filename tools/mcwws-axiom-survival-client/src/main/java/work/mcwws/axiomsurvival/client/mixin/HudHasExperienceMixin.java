package work.mcwws.axiomsurvival.client.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

/**
 * 在 Hud 渲染链上拦截 {@code hasExperience()}，确保 Editor 建造阶段选用经验条区域。
 */
@Mixin(Hud.class)
public class HudHasExperienceMixin {

    @Redirect(
            method = {"extractHotbarAndDecorations", "nextContextualInfoState"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;hasExperience()Z"
            )
    )
    private boolean mcwws$redirectHasExperience(MultiPlayerGameMode gameMode) {
        if (SurvivalEditorController.isLocalEditorActive()) {
            return SurvivalEditorController.shouldShowSurvivalHud();
        }
        return gameMode.hasExperience();
    }
}
