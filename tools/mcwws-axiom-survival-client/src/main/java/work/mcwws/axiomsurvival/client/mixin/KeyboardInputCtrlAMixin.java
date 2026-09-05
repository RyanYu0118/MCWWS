package work.mcwws.axiomsurvival.client.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.axiomsurvival.client.McwwsPathHotkeys;

/**
 * Editor 里 Ctrl+A 全选钢笔节点时，不要把 A 当成原版向左平移。
 * {@code keyPresses}/{@code moveVector} 在父类 {@link ClientInput} 上，不能对 {@link KeyboardInput} 做 {@code @Shadow}。
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputCtrlAMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void mcwws$lockLeftOnCtrlA(CallbackInfo ci) {
        if (!McwwsPathHotkeys.shouldSuppressStrafeLeft()) {
            return;
        }
        Input keys = this.keyPresses;
        if (!keys.left()) {
            return;
        }
        this.keyPresses = new Input(
                keys.forward(),
                keys.backward(),
                false,
                keys.right(),
                keys.jump(),
                keys.shift(),
                keys.sprint()
        );
        float forward = mcwws$impulse(this.keyPresses.forward(), this.keyPresses.backward());
        float strafe = mcwws$impulse(this.keyPresses.left(), this.keyPresses.right());
        this.moveVector = new Vec2(strafe, forward).normalized();
    }

    @Unique
    private static float mcwws$impulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }
}
