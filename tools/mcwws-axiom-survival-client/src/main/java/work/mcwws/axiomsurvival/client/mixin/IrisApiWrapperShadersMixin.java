package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.utils.IrisApiWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mcwws.axiomsurvival.client.ShaderPacks;

/**
 * {@code ChunkRenderOverrider} 只在 {@link IrisApiWrapper#isUsingShaders()} 为真时停用。
 * Vitrail 光影包开着时同样停用，避免 Sodium 挖掉整段区块后替换网格又被合成成透明。
 */
@Mixin(value = IrisApiWrapper.class, remap = false)
public class IrisApiWrapperShadersMixin {

    @Inject(method = "isUsingShaders", at = @At("RETURN"), cancellable = true, remap = false)
    private static void mcwws$vitrailCountsAsShaders(CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue()) && ShaderPacks.vitrailPackEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
