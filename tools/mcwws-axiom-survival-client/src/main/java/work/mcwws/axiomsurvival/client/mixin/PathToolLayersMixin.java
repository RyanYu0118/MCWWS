package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mcwws.axiomsurvival.client.McwwsGizmoGroup;
import work.mcwws.axiomsurvival.client.McwwsPathLayers;
import work.mcwws.axiomsurvival.client.PathClipboard;
import work.mcwws.axiomsurvival.client.PathLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 钢笔多路径图层：Ctrl+C/V 复制粘贴为新图层；重算时把其它图层追加进方块预览，互不串线。
 */
@Mixin(value = PathTool.class, remap = false)
public abstract class PathToolLayersMixin implements McwwsPathLayers {

    @Shadow
    private GizmoList gizmoList;

    @Shadow
    private List<PointConfig> pointConfigs;

    @Invoker("createDefaultPointConfig")
    protected abstract PointConfig mcwws$defaultPointConfig();

    @Invoker("recalculate")
    protected abstract void mcwws$invokeRecalculate();

    @Shadow
    public abstract void markDirty();

    @Unique
    private final List<PathLayer> mcwws$layers = new ArrayList<>();

    @Unique
    private int mcwws$activeLayer;

    @Unique
    private final PathClipboard mcwws$clipboard = new PathClipboard();

    @Unique
    private boolean mcwws$appendingOtherLayers;

    @Unique
    private int mcwws$layerSeq = 1;

    @Inject(method = "reset", at = @At("RETURN"), remap = false)
    private void mcwws$resetLayers(CallbackInfo ci) {
        mcwws$layers.clear();
        mcwws$activeLayer = 0;
        mcwws$layerSeq = 1;
        mcwws$layers.add(new PathLayer("图层 1"));
        mcwws$clipboard.clear();
    }

    @Redirect(
            method = "recalculate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/moulberry/axiom/render/regions/ChunkedBlockRegion;clear()V"
            ),
            remap = false
    )
    private void mcwws$skipClearWhenAppending(ChunkedBlockRegion region) {
        if (mcwws$appendingOtherLayers) {
            return;
        }
        try {
            ChunkedBlockRegion.class.getMethod("clear").invoke(region);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject(method = "recalculate", at = @At("RETURN"), remap = false)
    private void mcwws$appendOtherLayers(CallbackInfo ci) {
        if (mcwws$appendingOtherLayers) {
            return;
        }
        mcwws$ensureLayers();
        mcwws$captureActive();
        PathLayer activeBackup = mcwws$layers.get(mcwws$activeLayer).copy();
        int active = mcwws$activeLayer;
        try {
            for (int i = 0; i < mcwws$layers.size(); i++) {
                if (i == active) {
                    continue;
                }
                PathLayer layer = mcwws$layers.get(i);
                if (layer.isEmpty()) {
                    continue;
                }
                mcwws$loadLayer(layer);
                mcwws$appendingOtherLayers = true;
                try {
                    mcwws$invokeRecalculate();
                } finally {
                    mcwws$appendingOtherLayers = false;
                }
            }
        } finally {
            mcwws$loadLayer(activeBackup);
            mcwws$activeLayer = active;
        }
    }

    @Inject(method = "callAction", at = @At("HEAD"), cancellable = true, remap = false)
    private void mcwws$layerCopyPaste(
            UserAction action,
            Object data,
            CallbackInfoReturnable<UserAction.ActionResult> cir
    ) {
        if (action == UserAction.COPY || action == UserAction.DUPLICATE) {
            if (mcwws$copySelectionOrLayer()) {
                if (action == UserAction.DUPLICATE) {
                    mcwws$pasteAsNewLayer();
                }
                cir.setReturnValue(UserAction.ActionResult.USED_STOP);
            }
            return;
        }
        if (action == UserAction.PASTE) {
            if (!mcwws$clipboard.isEmpty()) {
                mcwws$pasteAsNewLayer();
                cir.setReturnValue(UserAction.ActionResult.USED_STOP);
            }
        }
    }

    @Inject(method = "displayImguiOptions", at = @At("RETURN"), remap = false)
    private void mcwws$layerPanel(CallbackInfo ci) {
        mcwws$ensureLayers();
        mcwws$captureActive();

        ImGui.separator();
        ImGui.textUnformatted("路径图层（互不串线，一起生成方块）");
        if (ImGui.button("复制图层")) {
            mcwws$copySelectionOrLayer();
        }
        ImGui.sameLine();
        if (ImGui.button("粘贴为新图层")) {
            if (!mcwws$clipboard.isEmpty()) {
                mcwws$pasteAsNewLayer();
            }
        }
        ImGui.sameLine();
        if (ImGui.button("新建空图层")) {
            mcwws$captureActive();
            mcwws$layers.add(new PathLayer("图层 " + (++mcwws$layerSeq)));
            mcwws$activeLayer = mcwws$layers.size() - 1;
            mcwws$loadLayer(mcwws$layers.get(mcwws$activeLayer));
            markDirty();
        }

        for (int i = 0; i < mcwws$layers.size(); i++) {
            PathLayer layer = mcwws$layers.get(i);
            boolean active = i == mcwws$activeLayer;
            String label = (active ? "▶ " : "  ") + layer.name + " (" + layer.points.size() + " 点)";
            if (ImGui.selectable(label + "##layer" + i, active)) {
                if (i != mcwws$activeLayer) {
                    mcwws$switchToLayer(i);
                }
            }
            ImGui.sameLine();
            if (ImGui.smallButton("删##dellayer" + i)) {
                mcwws$deleteLayer(i);
                break;
            }
        }
        ImGui.textUnformatted("Ctrl+C 复制当前选中/整层；Ctrl+V 粘贴为新图层。切换图层后可单独拖动。");
    }

    @Unique
    private void mcwws$ensureLayers() {
        if (mcwws$layers.isEmpty()) {
            PathLayer layer = new PathLayer("图层 1");
            mcwws$layers.add(layer);
            mcwws$activeLayer = 0;
            mcwws$layerSeq = 1;
        }
        if (mcwws$activeLayer < 0 || mcwws$activeLayer >= mcwws$layers.size()) {
            mcwws$activeLayer = 0;
        }
    }

    @Unique
    private void mcwws$captureActive() {
        mcwws$ensureLayers();
        PathLayer layer = mcwws$layers.get(mcwws$activeLayer);
        layer.points.clear();
        layer.configs.clear();
        List<Gizmo> gizmos = gizmoList.getGizmos();
        for (int i = 0; i < gizmos.size(); i++) {
            layer.points.add(gizmos.get(i).getTargetVec());
            if (i < pointConfigs.size()) {
                layer.configs.add(new PointConfig(pointConfigs.get(i)));
            } else {
                layer.configs.add(mcwws$defaultPointConfig());
            }
        }
    }

    @Unique
    private void mcwws$loadLayer(PathLayer layer) {
        gizmoList.clear();
        pointConfigs.clear();
        for (int i = 0; i < layer.points.size(); i++) {
            gizmoList.addGizmo(layer.points.get(i));
            if (i < layer.configs.size()) {
                pointConfigs.add(new PointConfig(layer.configs.get(i)));
            } else {
                pointConfigs.add(mcwws$defaultPointConfig());
            }
        }
    }

    @Unique
    private void mcwws$switchToLayer(int index) {
        mcwws$captureActive();
        mcwws$activeLayer = index;
        mcwws$loadLayer(mcwws$layers.get(index));
        if (!gizmoList.isEmpty()) {
            ((McwwsGizmoGroup) (Object) gizmoList).mcwwsSelectAll();
        }
        markDirty();
    }

    @Unique
    private void mcwws$deleteLayer(int index) {
        mcwws$captureActive();
        if (mcwws$layers.size() <= 1) {
            PathLayer only = mcwws$layers.get(0);
            only.points.clear();
            only.configs.clear();
            gizmoList.clear();
            pointConfigs.clear();
            markDirty();
            return;
        }
        mcwws$layers.remove(index);
        if (mcwws$activeLayer > index) {
            mcwws$activeLayer--;
        } else if (mcwws$activeLayer >= mcwws$layers.size()) {
            mcwws$activeLayer = mcwws$layers.size() - 1;
        } else if (mcwws$activeLayer == index) {
            if (mcwws$activeLayer >= mcwws$layers.size()) {
                mcwws$activeLayer = mcwws$layers.size() - 1;
            }
        }
        mcwws$loadLayer(mcwws$layers.get(mcwws$activeLayer));
        markDirty();
    }

    @Unique
    private boolean mcwws$copySelectionOrLayer() {
        mcwws$ensureLayers();
        mcwws$captureActive();
        List<Integer> indices = ((McwwsGizmoGroup) (Object) gizmoList).mcwwsCopyIndicesAscending();
        if (indices.isEmpty()) {
            return false;
        }
        List<Vec3> pts = new ArrayList<>();
        List<PointConfig> cfgs = new ArrayList<>();
        PathLayer active = mcwws$layers.get(mcwws$activeLayer);
        for (int i : indices) {
            if (i < 0 || i >= active.points.size()) {
                continue;
            }
            pts.add(active.points.get(i));
            if (i < active.configs.size()) {
                cfgs.add(new PointConfig(active.configs.get(i)));
            } else {
                cfgs.add(mcwws$defaultPointConfig());
            }
        }
        if (pts.isEmpty()) {
            return false;
        }
        mcwws$clipboard.set(pts, cfgs);
        return true;
    }

    @Unique
    private void mcwws$pasteAsNewLayer() {
        if (mcwws$clipboard.isEmpty()) {
            return;
        }
        mcwws$captureActive();
        PathLayer layer = mcwws$clipboard.toLayer("图层 " + (++mcwws$layerSeq));
        mcwws$layers.add(layer);
        mcwws$activeLayer = mcwws$layers.size() - 1;
        mcwws$loadLayer(layer);
        if (!gizmoList.isEmpty()) {
            ((McwwsGizmoGroup) (Object) gizmoList).mcwwsSelectAll();
        }
        markDirty();
    }

    @Override
    public List<PathLayer> mcwwsAllLayers() {
        mcwws$ensureLayers();
        mcwws$captureActive();
        List<PathLayer> copy = new ArrayList<>(mcwws$layers.size());
        for (PathLayer layer : mcwws$layers) {
            copy.add(layer.copy());
        }
        return copy;
    }

    @Override
    public int mcwwsActiveLayerIndex() {
        return mcwws$activeLayer;
    }

    @Override
    public void mcwwsReplaceAllLayers(List<PathLayer> layers, int activeIndex) {
        mcwws$layers.clear();
        if (layers == null || layers.isEmpty()) {
            mcwws$layers.add(new PathLayer("图层 1"));
            mcwws$activeLayer = 0;
            mcwws$layerSeq = 1;
        } else {
            for (PathLayer layer : layers) {
                mcwws$layers.add(layer.copy());
            }
            mcwws$activeLayer = Math.max(0, Math.min(activeIndex, mcwws$layers.size() - 1));
            mcwws$layerSeq = mcwws$layers.size();
        }
        mcwws$loadLayer(mcwws$layers.get(mcwws$activeLayer));
        if (!gizmoList.isEmpty()) {
            ((McwwsGizmoGroup) (Object) gizmoList).mcwwsSelectAll();
        }
        markDirty();
    }
}
