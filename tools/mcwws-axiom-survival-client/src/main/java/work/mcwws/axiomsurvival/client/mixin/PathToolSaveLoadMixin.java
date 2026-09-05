package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.tools.modelling.GizmoList;
import com.moulberry.axiom.tools.path.PathTool;
import com.moulberry.axiom.tools.path.PointConfig;
import imgui.moulberry92.ImGui;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.axiomsurvival.client.McwwsGizmoGroup;
import work.mcwws.axiomsurvival.client.PathLibrary;

import java.util.ArrayList;
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
            List<Vec3> points = new ArrayList<>();
            for (Gizmo gizmo : gizmoList.getGizmos()) {
                points.add(gizmo.getTargetVec());
            }
            PathLibrary.saveDialog(points);
        }
        ImGui.sameLine();
        if (ImGui.button("导入轨迹")) {
            PathLibrary.openDialog(this::mcwws$applyPoints);
        }
        ImGui.sameLine();
        if (ImGui.button("全选节点")) {
            ((McwwsGizmoGroup) (Object) gizmoList).mcwwsSelectAll();
        }
        ImGui.textUnformatted("Ctrl+A 全选（不会向左平移）。Delete 删除所有金色选中点。按住 Ctrl 会暂时藏起节点，松开后再拖 Y 轴即可整体上移。");
    }

    @Inject(
            method = "callAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/moulberry/axiom/tools/modelling/GizmoList;delete()V"
            ),
            remap = false
    )
    private void mcwws$trimConfigsOnDeleteKey(UserAction action, Object data, CallbackInfo ci) {
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

    @Unique
    private void mcwws$applyPoints(List<Vec3> points) {
        gizmoList.clear();
        pointConfigs.clear();
        for (Vec3 point : points) {
            gizmoList.addGizmo(point);
            pointConfigs.add(mcwws$defaultPointConfig());
        }
        if (!gizmoList.isEmpty()) {
            ((McwwsGizmoGroup) (Object) gizmoList).mcwwsSelectAll();
        }
        markDirty();
    }
}
