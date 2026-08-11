package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.integration.ServerIntegration;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

/**
 * 让 Axiom 正常 {@code setLocalMode}（供 EditorUI 校验），仅跟踪会话；发往服务端的模式包由
 * {@link ServerIntegrationSendGamemodeMixin} 拦截。
 */
@Mixin(value = ServerIntegration.class, remap = false)
public class ServerIntegrationChangeGameModeMixin {

    @Inject(method = "changeGameMode", at = @At("HEAD"), remap = false)
    private static void mcwws$beforeChangeGameMode(GameType gameType, CallbackInfo ci) {
        if (!SurvivalEditorController.isServerSupported()) {
            return;
        }
        if (gameType == GameType.SPECTATOR && SurvivalEditorController.isLocalEditorActive()) {
            SurvivalEditorController.captureMenuOpenPose();
        } else if ((gameType == GameType.SPECTATOR || gameType == GameType.CREATIVE)
                && !SurvivalEditorController.isLocalEditorActive()) {
            SurvivalEditorController.captureStoredModeBeforeEnter();
        }
    }

    @Inject(method = "changeGameMode", at = @At("RETURN"), remap = false)
    private static void mcwws$afterChangeGameMode(GameType gameType, CallbackInfo ci) {
        if (!SurvivalEditorController.isServerSupported()) {
            return;
        }
        if (gameType == GameType.SPECTATOR || gameType == GameType.CREATIVE) {
            SurvivalEditorController.onEditorGamemodeChange(gameType);
            if (gameType == GameType.CREATIVE) {
                SurvivalEditorController.onEnterBuildMode();
            }
            return;
        }
        if (SurvivalEditorController.isLocalEditorActive()
                && (gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE)) {
            SurvivalEditorController.onEditorExit();
        }
    }
}
