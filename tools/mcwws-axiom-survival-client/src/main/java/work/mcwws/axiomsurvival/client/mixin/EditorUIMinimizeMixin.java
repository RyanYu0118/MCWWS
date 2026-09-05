package work.mcwws.axiomsurvival.client.mixin;

import com.mojang.blaze3d.platform.Window;
import com.moulberry.axiom.editor.EditorUI;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 窗口最小化时跳过 Axiom EditorUI 整帧 ImGui/合成渲染，避免 0 尺寸视口反复分配导致内存暴涨与 GPU 空转。
 */
@Mixin(value = EditorUI.class, remap = false)
public class EditorUIMinimizeMixin {

    @Inject(method = "drawOverlay", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mcwws$skipOverlayWhenMinimized(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        Window window = mc.getWindow();
        if (window != null && (window.isIconified() || window.isMinimized())) {
            ci.cancel();
        }
    }
}
