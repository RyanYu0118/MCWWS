package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.editor.EditorUI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

@Mixin(value = EditorUI.class, remap = false)
public class EditorUIDisableMixin {

    /**
     * {@code disable()} 在 {@code separateFlightSpeeds} 开启时会先把当前速度存成「编辑器用」，
     * 再换回进菜单前的游戏速度。必须在那之前记下玩家刚调的值。
     */
    @Inject(method = "disable", at = @At("HEAD"), remap = false)
    private static void mcwws$beforeDisable(CallbackInfo ci) {
        SurvivalEditorController.captureEditorFlySpeedBeforeMenuClose();
    }

    @Inject(method = "disable", at = @At("TAIL"), remap = false)
    private static void mcwws$afterDisable(CallbackInfo ci) {
        SurvivalEditorController.finalizeMenuClose();
    }
}
