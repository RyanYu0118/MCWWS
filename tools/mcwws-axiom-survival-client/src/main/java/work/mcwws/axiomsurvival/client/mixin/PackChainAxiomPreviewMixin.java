package work.mcwws.axiomsurvival.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mcwws.axiomsurvival.client.AxiomPreviewRemount;

/**
 * 若 Vitrail 在 {@code renderLevel} 返回之后仍执行 {@code PackChain.draw}，这里再补一次。
 * 无 Vitrail 时本 mixin 配置为非必需，不会阻止启动。
 */
@Mixin(targets = "dev.vitrail.render.PackChain", remap = false)
public class PackChainAxiomPreviewMixin {

    @Inject(method = "draw", at = @At("RETURN"), remap = false)
    private static void mcwws$remountAfterPackDraw(CallbackInfoReturnable<Boolean> cir) {
        GameRenderer renderer = Minecraft.getInstance().gameRenderer;
        if (renderer != null) {
            AxiomPreviewRemount.remountOncePerTick(renderer);
        }
    }
}
