package work.mcwws.axiomsurvival.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.tools.modelling.GizmoList;
import imgui.moulberry92.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mcwws.axiomsurvival.client.McwwsGizmoGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * Axiom 钢笔/建模的 {@link GizmoList} 只有一个 {@code activeGizmo}，拖轴只能动当前节点。
 * Shift+点击加选；Ctrl+A 或工具面板「全选节点」选中全部。拖当前节点时整组平移。
 */
@Mixin(value = GizmoList.class, remap = false)
public abstract class GizmoListGroupTranslateMixin implements McwwsGizmoGroup {

    private static final int MCWWS_DEFAULT_CENTER = 0xFFFFFF;
    private static final int MCWWS_GROUP_CENTER = 0xFFCC44;

    @Shadow
    private List<Gizmo> gizmos;

    @Shadow
    private int activeGizmo;

    @Shadow
    public abstract void setActiveGizmo(int index);

    @Shadow
    public abstract void clear();

    @Unique
    private final TreeSet<Integer> mcwws$group = new TreeSet<>();

    @Unique
    private int mcwws$previousActive = -1;

    @Unique
    private Vec3 mcwws$dragOrigin;

    @Unique
    private boolean mcwws$keepGroup;

    @Unique
    private boolean mcwws$aWasDown;

    @Invoker("pushHistory")
    protected abstract void mcwws$pushHistory(
            GizmoList.GizmoListHistoryAction backwards,
            GizmoList.GizmoListHistoryAction forwards,
            BlockPos pos,
            String description,
            boolean applyForwards
    );

    @Inject(method = "setActiveGizmo", at = @At("HEAD"), remap = false)
    private void mcwws$rememberActive(int index, CallbackInfo ci) {
        mcwws$previousActive = activeGizmo;
    }

    @Inject(method = "setActiveGizmo", at = @At("RETURN"), remap = false)
    private void mcwws$updateGroup(int index, CallbackInfo ci) {
        if (mcwws$keepGroup) {
            mcwws$refreshColours();
            return;
        }
        if (index < 0) {
            mcwws$group.clear();
            mcwws$dragOrigin = null;
            mcwws$refreshColours();
            return;
        }
        if (Minecraft.getInstance().hasShiftDown()) {
            if (mcwws$previousActive >= 0) {
                mcwws$group.add(mcwws$previousActive);
            }
            mcwws$group.add(index);
        } else {
            mcwws$group.clear();
        }
        mcwws$dragOrigin = null;
        mcwws$refreshColours();
    }

    @Inject(method = "addGizmo", at = @At("HEAD"), remap = false)
    private void mcwws$shiftGroupOnInsert(Vec3 position, CallbackInfo ci) {
        if (mcwws$group.isEmpty()) {
            return;
        }
        int insertAt = activeGizmo >= 0 ? activeGizmo + 1 : gizmos.size();
        TreeSet<Integer> shifted = new TreeSet<>();
        for (int i : mcwws$group) {
            shifted.add(i >= insertAt ? i + 1 : i);
        }
        mcwws$group.clear();
        mcwws$group.addAll(shifted);
    }

    @Inject(method = "clear", at = @At("HEAD"), remap = false)
    private void mcwws$clearGroup(CallbackInfo ci) {
        mcwws$group.clear();
        mcwws$dragOrigin = null;
    }

    @Inject(method = "updateGizmos", at = @At("HEAD"), remap = false)
    private void mcwws$ctrlASelectAll(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        boolean aDown = InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_A);
        boolean pressed = aDown && !mcwws$aWasDown;
        mcwws$aWasDown = aDown;
        if (!pressed || !mc.hasControlDown()) {
            return;
        }
        if (ImGui.getIO().getWantTextInput()) {
            return;
        }
        mcwwsSelectAll();
    }

    @Inject(method = "updateGizmos", at = @At("RETURN"), remap = false)
    private void mcwws$followAfterUpdate(CallbackInfoReturnable<Boolean> cir) {
        mcwws$followActiveDelta();
    }

    @Inject(method = "handleScroll", at = @At("RETURN"), remap = false)
    private void mcwws$followAfterScroll(int scrollX, int scrollY, CallbackInfoReturnable<Boolean> cir) {
        mcwws$followActiveDelta();
    }

    @Inject(method = "delete", at = @At("HEAD"), cancellable = true, remap = false)
    private void mcwws$deleteGroup(CallbackInfo ci) {
        if (mcwws$group.size() <= 1) {
            return;
        }
        mcwws$deleteSelectedGroup();
        ci.cancel();
    }

    @Override
    public void mcwwsSelectAll() {
        if (gizmos.isEmpty()) {
            return;
        }
        if (activeGizmo < 0) {
            mcwws$keepGroup = true;
            setActiveGizmo(0);
            mcwws$keepGroup = false;
        }
        mcwws$group.clear();
        for (int i = 0; i < gizmos.size(); i++) {
            mcwws$group.add(i);
        }
        mcwws$dragOrigin = null;
        mcwws$refreshColours();
    }

    @Override
    public List<Integer> mcwwsSelectedDescending() {
        if (mcwws$group.size() <= 1) {
            return List.of();
        }
        List<Integer> indices = new ArrayList<>(mcwws$group);
        indices.sort(Comparator.reverseOrder());
        return indices;
    }

    @Unique
    private void mcwws$deleteSelectedGroup() {
        if (mcwws$group.size() <= 1) {
            return;
        }
        if (mcwws$group.size() == gizmos.size()) {
            clear();
            return;
        }
        List<Integer> indices = mcwwsSelectedDescending();
        mcwws$keepGroup = true;
        try {
            for (int i : indices) {
                if (i < 0 || i >= gizmos.size()) {
                    continue;
                }
                Gizmo gizmo = gizmos.get(i);
                mcwws$pushHistory(
                        new GizmoList.AddGizmo(i, gizmo.getTargetVec()),
                        new GizmoList.RemoveGizmo(i),
                        gizmo.getTargetPosition(),
                        "#" + (i + 1),
                        true
                );
            }
        } finally {
            mcwws$keepGroup = false;
        }
        mcwws$group.clear();
        mcwws$dragOrigin = null;
        int next = activeGizmo;
        if (next >= gizmos.size()) {
            next = gizmos.isEmpty() ? -1 : gizmos.size() - 1;
        }
        if (next != activeGizmo) {
            mcwws$keepGroup = true;
            setActiveGizmo(next);
            mcwws$keepGroup = false;
        } else if (next >= 0) {
            mcwws$group.add(next);
        }
        mcwws$refreshColours();
    }

    @Unique
    private void mcwws$followActiveDelta() {
        if (activeGizmo < 0 || activeGizmo >= gizmos.size() || mcwws$group.isEmpty()) {
            mcwws$dragOrigin = activeGizmo >= 0 && activeGizmo < gizmos.size()
                    ? gizmos.get(activeGizmo).getTargetVec()
                    : null;
            mcwws$refreshColours();
            return;
        }
        Gizmo active = gizmos.get(activeGizmo);
        Vec3 now = active.getTargetVec();
        if (mcwws$dragOrigin != null && !now.equals(mcwws$dragOrigin)) {
            Vec3 delta = now.subtract(mcwws$dragOrigin);
            for (int i : new ArrayList<>(mcwws$group)) {
                if (i == activeGizmo || i < 0 || i >= gizmos.size()) {
                    continue;
                }
                Gizmo extra = gizmos.get(i);
                Vec3 old = extra.getTargetVec();
                Vec3 neu = old.add(delta);
                extra.moveToVec(neu);
                mcwws$pushHistory(
                        new GizmoList.MoveGizmo(i, old),
                        new GizmoList.MoveGizmo(i, neu),
                        BlockPos.containing(neu),
                        "#" + (i + 1),
                        false
                );
            }
        }
        mcwws$dragOrigin = now;
        mcwws$refreshColours();
    }

    @Unique
    private void mcwws$refreshColours() {
        for (int i = 0; i < gizmos.size(); i++) {
            Gizmo gizmo = gizmos.get(i);
            boolean selected = mcwws$group.contains(i);
            gizmo.centerColour = selected ? MCWWS_GROUP_CENTER : MCWWS_DEFAULT_CENTER;
        }
    }
}
