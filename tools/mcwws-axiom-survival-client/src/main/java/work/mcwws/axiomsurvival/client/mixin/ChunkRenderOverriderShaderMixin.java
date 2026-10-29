package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.utils.IrisApiWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code disabled} 要等本帧 {@code willRender} 才置位，Sodium 组命令缓冲可能更早。
 * 光影已开时同一帧就不要把整段区块从真实网格里摘掉。
 */
@Mixin(value = ChunkRenderOverrider.class, remap = false)
public class ChunkRenderOverriderShaderMixin {

    @Inject(method = "isOverridingSection", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mcwws$keepRealSectionUnderShaders(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (IrisApiWrapper.isUsingShaders()) {
            cir.setReturnValue(false);
        }
    }
}
