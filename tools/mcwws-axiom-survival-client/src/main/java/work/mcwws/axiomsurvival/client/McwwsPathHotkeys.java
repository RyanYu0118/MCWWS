package work.mcwws.axiomsurvival.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

/**
 * 钢笔 Ctrl+A 全选时，原版仍把 A 读成「向左平移」。Editor 会话内按住 Ctrl+A 时锁住左移。
 */
public final class McwwsPathHotkeys {

    private McwwsPathHotkeys() {
    }

    public static boolean shouldSuppressStrafeLeft() {
        if (!SurvivalEditorController.isLocalEditorActive()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.hasControlDown()) {
            return false;
        }
        return InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_A);
    }
}
