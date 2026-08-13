package work.mcwws.axiomsurvival.client.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

/**
 * 26.2 的 {@code Player.canEat} 在 {@code abilities.invulnerable} 为 true 时直接允许进食。
 * Axiom 把本地切成创造会打开无敌，饱食度满了客户端仍会播吃东西动作；服务端是生存，动作会空放。
 */
@Mixin(Player.class)
public class PlayerCanEatMixin {

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void mcwws$eatLikeSurvival(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        if (!SurvivalEditorController.shouldActLikeSurvival()) {
            return;
        }
        Player self = (Player) (Object) this;
        cir.setReturnValue(ignoreHunger || self.getFoodData().needsFood());
    }
}
