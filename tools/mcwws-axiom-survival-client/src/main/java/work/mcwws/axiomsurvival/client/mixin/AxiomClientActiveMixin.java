package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.AxiomClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

/**
 * {@code isAxiomActive} 末尾要求本地模式恰好等于创造/旁观，生存下 Axiom 整体判定为未激活，
 * 工具槽（移动/克隆/堆叠）与建筑工具都不出现。此处只改这一次模式比较，
 * Axiom 自身的服务端支持与权限判定全部保留。
 */
@Mixin(value = AxiomClient.class, remap = false)
public class AxiomClientActiveMixin {

    @Redirect(
            method = "isAxiomActive(Lnet/minecraft/world/level/GameType;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;"
                            + "getPlayerMode()Lnet/minecraft/world/level/GameType;"
            ),
            remap = false
    )
    private static GameType mcwws$virtualPlayerMode(MultiPlayerGameMode gameMode) {
        GameType real = gameMode.getPlayerMode();
        if (real != GameType.SURVIVAL && real != GameType.ADVENTURE) {
            return real;
        }
        if (!SurvivalEditorController.shouldSpoofAxiomActive()) {
            return real;
        }
        return SurvivalEditorController.virtualAxiomMode();
    }
}
