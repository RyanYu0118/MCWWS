package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.IrisApiWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 移动选区的预览固定用 0.75 透明度，再乘进光影的半透明合成里就会发透。
 * 光影开着时改成不透明，关掉光影后仍保留原来的半透明预览。
 */
@Mixin(value = ChunkedBlockRegion.class, remap = false)
public class ChunkedBlockRegionOpacityMixin {

    @ModifyVariable(
            method = "render(Lcom/moulberry/axiom/render/AxiomWorldRenderContext;Lnet/minecraft/world/phys/Vec3;Lorg/joml/Quaternionf;FFZLcom/mojang/blaze3d/pipeline/RenderTarget;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private float mcwws$solidPreviewUnderShaders(float blockOpacity) {
        if (blockOpacity > 0.01F && blockOpacity < 0.999F && IrisApiWrapper.isUsingShaders()) {
            return 1.0F;
        }
        return blockOpacity;
    }
}
