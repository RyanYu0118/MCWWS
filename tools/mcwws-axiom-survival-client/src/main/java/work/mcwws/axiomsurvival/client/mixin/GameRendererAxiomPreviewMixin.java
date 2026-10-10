package work.mcwws.axiomsurvival.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.axiomsurvival.client.AxiomPreviewRemount;

/**
 * 在整帧世界渲染（含 Vitrail 对 {@code renderLevel} 内部的合成）结束后重挂 Axiom 预览，
 * 并赶在 HUD 之前，避免幽灵叠到界面上。
 */
@Mixin(GameRenderer.class)
public class GameRendererAxiomPreviewMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void mcwws$resetPreviewRemount(CallbackInfo ci) {
        AxiomPreviewRemount.beginFrame();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void mcwws$remountAfterLevel(CallbackInfo ci) {
        AxiomPreviewRemount.remountOncePerTick((GameRenderer) (Object) this);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel()V",
                    shift = At.Shift.AFTER
            )
    )
    private void mcwws$remountAfterRenderLevelCall(CallbackInfo ci) {
        AxiomPreviewRemount.remountOncePerTick((GameRenderer) (Object) this);
    }
}
