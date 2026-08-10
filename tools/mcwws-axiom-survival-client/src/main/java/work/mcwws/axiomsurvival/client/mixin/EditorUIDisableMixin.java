package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.editor.EditorUI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

@Mixin(value = EditorUI.class, remap = false)
public class EditorUIDisableMixin {

    @Inject(method = "disable", at = @At("HEAD"), remap = false)
    private void mcwws$disable(CallbackInfo ci) {
        if (SurvivalEditorController.isLocalEditorActive()) {
            SurvivalEditorController.onEditorExit();
        }
    }
}
