package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.gizmo.ExtrudedGizmo;
import com.moulberry.axiom.tools.modelling.GizmoList;
import com.moulberry.axiom.tools.path.PathTool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 官方按 Enter 确认会先 {@code pastePath()}，再新建空 {@code GizmoList}。
 * 超视距或服务端拒包时方块没落下，节点却已被清掉。这里让确认与 Paste Copy 一样保留轨迹。
 */
@Mixin(value = PathTool.class, remap = false)
public abstract class PathToolKeepPathMixin {

    @Shadow
    private ExtrudedGizmo extrudedGizmo;

    @Shadow
    private GizmoList gizmoList;

    @Invoker("pastePath")
    protected abstract void mcwws$invokePastePath();

    @Inject(method = "callAction", at = @At("HEAD"), cancellable = true, remap = false)
    private void mcwws$keepPathOnConfirm(
            UserAction action,
            Object data,
            CallbackInfoReturnable<UserAction.ActionResult> cir
    ) {
        if (action != UserAction.ENTER) {
            return;
        }
        if (extrudedGizmo != null || gizmoList.isEmpty()) {
            return;
        }
        mcwws$invokePastePath();
        cir.setReturnValue(UserAction.ActionResult.USED_STOP);
    }
}
