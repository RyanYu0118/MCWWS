package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.tools.modelling.GizmoList;
import com.moulberry.axiom.tools.path.PathTool;
import com.moulberry.axiom.tools.path.PointConfig;
import imgui.moulberry92.ImGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mcwws.axiomsurvival.client.McwwsGizmoGroup;
import work.mcwws.axiomsurvival.client.McwwsPathLayers;
import work.mcwws.axiomsurvival.client.PathLibrary;

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

    @Inject(method = "displayImguiOptions", at = @At("RETURN"), remap = false)
    private void mcwws$pathLibraryButtons(CallbackInfo ci) {
        ImGui.separator();
        if (ImGui.button("保存轨迹")) {
            McwwsPathLayers layers = (McwwsPathLayers) (Object) this;
            PathLibrary.saveDialogLayers(layers.mcwwsAllLayers(), layers.mcwwsActiveLayerIndex());
        }
        ImGui.sameLine();
        if (ImGui.button("导入轨迹")) {
            PathLibrary.openDialogLayers((loaded, active) ->
                    ((McwwsPathLayers) (Object) this).mcwwsReplaceAllLayers(loaded, active));
        }
        ImGui.sameLine();
        if (ImGui.button("全选节点")) {
            ((McwwsGizmoGroup) (Object) gizmoList).mcwwsSelectAll();
        }
        ImGui.textUnformatted("Ctrl+A 全选（不会向左平移）。Delete 删除所有金色选中点。Ctrl+C/V 复制粘贴为新图层。");
    }

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
