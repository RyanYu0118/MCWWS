package work.mcwws.axiomsurvival.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

/**
 * 本地创造会让挖掘走 {@code instabuild} 秒破路径，只在客户端移除方块，
 * 生存服务端不会认可，方块随即回填。会话内改走生存挖掘进度逻辑。
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMiningMixin {

    private static final String INSTABUILD = "Lnet/minecraft/world/entity/player/Abilities;instabuild:Z";

    /** ASM 不在编译类路径上，此处直接写 {@code Opcodes.GETFIELD} 的值 */
    private static final int GETFIELD = 180;

    @Redirect(method = "startDestroyBlock", at = @At(value = "FIELD", target = INSTABUILD, opcode = GETFIELD))
    private boolean mcwws$startDestroyInstabuild(Abilities abilities) {
        return abilities.instabuild && !SurvivalEditorController.shouldMineLikeSurvival();
    }

    @Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = INSTABUILD, opcode = GETFIELD))
    private boolean mcwws$continueDestroyInstabuild(Abilities abilities) {
        return abilities.instabuild && !SurvivalEditorController.shouldMineLikeSurvival();
    }
}
