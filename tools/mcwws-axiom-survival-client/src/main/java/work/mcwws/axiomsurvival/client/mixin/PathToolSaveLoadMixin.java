package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.tools.modelling.GizmoList;
import com.moulberry.axiom.tools.path.PathTool;
import com.moulberry.axiom.tools.path.PointConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mcwws.axiomsurvival.client.McwwsGizmoGroup;

import java.util.List;

@Mixin(value = PathTool.class, remap = false)
public abstract class PathToolSaveLoadMixin {

    @Shadow
    private GizmoList gizmoList;

    @Shadow
    private List<PointConfig> pointConfigs;

    @Invoker("createDefaultPointConfig")
    protected abstract PointConfig mcwws$defaultPointConfig();

    @Shadow
    public abstract void markDirty();

    @Inject(
            method = "callAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/moulberry/axiom/tools/modelling/GizmoList;delete()V"
            ),
            remap = false
    )
    private void mcwws$trimConfigsOnDeleteKey(UserAction action, Object data, CallbackInfoReturnable<UserAction.ActionResult> cir) {
        mcwws$trimSelectedPointConfigs();
    }

    @Inject(
            method = "displayImguiOptions",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/moulberry/axiom/tools/modelling/GizmoList;delete()V"
            ),
            remap = false
    )
    private void mcwws$trimConfigsOnDeleteButton(CallbackInfo ci) {
        mcwws$trimSelectedPointConfigs();
    }

    @Unique
    private void mcwws$trimSelectedPointConfigs() {
        List<Integer> indices = ((McwwsGizmoGroup) (Object) gizmoList).mcwwsSelectedDescending();
        if (indices.isEmpty()) {
            return;
        }
        if (indices.size() == gizmoList.size()) {
            pointConfigs.clear();
            return;
        }
        for (int i : indices) {
            if (i >= 0 && i < pointConfigs.size()) {
                pointConfigs.remove(i);
            }
        }
    }
}
