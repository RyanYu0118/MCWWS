package work.mcwws.axiomsurvival.client.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.axiomsurvival.client.SchematicDropHandler;

import java.util.List;

/**
 * 26.3 起 SDL 拖放经 {@link MouseHandler#onDrop} 转给当前 Screen；
 * Axiom Editor 无 Screen，在此拦截投影文件。
 */
@Mixin(MouseHandler.class)
public class MouseHandlerDropMixin {

    @Inject(method = "onDrop", at = @At("HEAD"), cancellable = true)
    private void mcwws$schematicDrop(long window, List<String> files, CallbackInfo ci) {
        if (SchematicDropHandler.tryHandle(files)) {
            ci.cancel();
        }
    }
}
