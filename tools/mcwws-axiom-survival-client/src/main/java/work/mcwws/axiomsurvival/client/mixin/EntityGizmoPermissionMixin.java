package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

/**
 * 生存里谎报 {@code isAxiomActive} 后，展示实体/标记会画出可拖动小方块并抢走右键。
 * 无权限时让这两处操纵器以为 Axiom 未激活，走原有清理分支，点击交回原版实体交互。
 */
@Mixin(value = {DisplayEntityManipulator.class, MarkerEntityManipulator.class}, remap = false)
public class EntityGizmoPermissionMixin {

    @Redirect(
            method = {"tick", "render", "callAction"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/moulberry/axiom/AxiomClient;isAxiomActive()Z"
            ),
            remap = false,
            require = 0
    )
    private static boolean mcwws$gizmosNeedPermission() {
        return SurvivalEditorController.shouldShowEntityGizmos() && AxiomClient.isAxiomActive();
    }
}
