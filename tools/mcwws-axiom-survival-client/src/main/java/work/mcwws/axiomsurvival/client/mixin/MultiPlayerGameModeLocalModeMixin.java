package work.mcwws.axiomsurvival.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

/**
 * 服务端仍为生存时会下发 {@code CHANGE_GAME_MODE}，阻止其覆盖 Editor 会话内的本地旁观/创造。
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeLocalModeMixin {

    @Inject(method = "setLocalMode(Lnet/minecraft/world/level/GameType;)V", at = @At("HEAD"), cancellable = true)
    private void mcwws$guardLocalMode(GameType gameType, CallbackInfo ci) {
        if (!SurvivalEditorController.shouldSuppressGamemodeSync()) {
            return;
        }
        if (gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE) {
            ci.cancel();
        }
    }

    @Inject(
            method = "setLocalMode(Lnet/minecraft/world/level/GameType;Lnet/minecraft/world/level/GameType;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mcwws$guardLocalModeWithPrevious(GameType gameType, GameType previousGameType, CallbackInfo ci) {
        if (!SurvivalEditorController.shouldSuppressGamemodeSync()) {
            return;
        }
        if (gameType == GameType.SURVIVAL || gameType == GameType.ADVENTURE) {
            ci.cancel();
        }
    }

    @Inject(method = "setLocalMode(Lnet/minecraft/world/level/GameType;)V", at = @At("TAIL"))
    private void mcwws$afterLocalMode(GameType gameType, CallbackInfo ci) {
        SurvivalEditorController.afterLocalModeChanged(gameType);
    }

    @Inject(
            method = "setLocalMode(Lnet/minecraft/world/level/GameType;Lnet/minecraft/world/level/GameType;)V",
            at = @At("TAIL")
    )
    private void mcwws$afterLocalModeWithPrevious(GameType gameType, GameType previousGameType, CallbackInfo ci) {
        SurvivalEditorController.afterLocalModeChanged(gameType);
    }
}
