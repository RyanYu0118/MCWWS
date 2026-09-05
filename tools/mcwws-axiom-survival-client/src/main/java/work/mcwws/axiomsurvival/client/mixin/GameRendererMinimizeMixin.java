package work.mcwws.axiomsurvival.client.mixin;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 窗口最小化时跳过整帧渲染，避免 Axiom 编辑界面打开时最小化仍满载 GPU。
 */
@Mixin(GameRenderer.class)
public class GameRendererMinimizeMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void mcwws$skipRenderWhenMinimized(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        Window window = Minecraft.getInstance().getWindow();
        if (window != null && (window.isIconified() || window.isMinimized())) {
            ci.cancel();
        }
    }
}
