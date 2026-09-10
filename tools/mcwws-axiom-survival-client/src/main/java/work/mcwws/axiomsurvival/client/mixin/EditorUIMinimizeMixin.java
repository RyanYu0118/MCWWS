package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.editor.EditorUI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

/**
 * 最小化时停掉 Axiom Editor 的 ImGui 叠加层，并强制 {@code isActive()==false}。
 * <p>
 * 根因：仅 cancel {@code drawOverlay} 时 {@code activeLastFrame} 仍为 true，
 * Axiom {@code MixinWindow} 会继续伪造 getWidth/Height，与真实 0×0 framebuffer
 * 冲突，Iris/主渲染每帧狂建缓冲导致原生内存暴涨。
 */
@Mixin(value = EditorUI.class, remap = false)
public class EditorUIMinimizeMixin {

    @Inject(method = "drawOverlay", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mcwws$skipOverlayWhenMinimized(CallbackInfo ci) {
        if (SurvivalEditorController.isClientWindowMinimized()) {
            ci.cancel();
        }
    }

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mcwws$forceInactiveWhenMinimized(CallbackInfoReturnable<Boolean> cir) {
        if (SurvivalEditorController.isClientWindowMinimized()) {
            cir.setReturnValue(false);
        }
    }
}
