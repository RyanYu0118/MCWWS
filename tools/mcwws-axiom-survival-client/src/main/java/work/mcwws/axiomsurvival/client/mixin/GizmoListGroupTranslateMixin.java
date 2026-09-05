package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.tools.modelling.GizmoList;
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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Axiom 钢笔/建模的 {@link GizmoList} 只有一个 {@code activeGizmo}，拖轴只能动当前节点。
 * Shift+点击把节点加进一组，拖动当前节点时按同一位移平移其余选中节点。
 */
@Mixin(value = GizmoList.class, remap = false)
public abstract class GizmoListGroupTranslateMixin {

    private static final int MCWWS_DEFAULT_CENTER = 0xFFFFFF;
    private static final int MCWWS_GROUP_CENTER = 0xFFCC44;

    @Shadow
    private List<Gizmo> gizmos;

    @Shadow
    private int activeGizmo;

    @Unique
    private final TreeSet<Integer> mcwws$group = new TreeSet<>();

    @Unique
    private int mcwws$previousActive = -1;

    @Unique
    private Vec3 mcwws$dragOrigin;

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

    @Inject(method = "updateGizmos", at = @At("RETURN"), remap = false)
    private void mcwws$followAfterUpdate(CallbackInfoReturnable<Boolean> cir) {
        mcwws$followActiveDelta();
    }

    @Inject(method = "handleScroll", at = @At("RETURN"), remap = false)
    private void mcwws$followAfterScroll(int scrollX, int scrollY, CallbackInfoReturnable<Boolean> cir) {
        mcwws$followActiveDelta();
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
            boolean extra = i != activeGizmo && mcwws$group.contains(i);
            gizmo.centerColour = extra ? MCWWS_GROUP_CENTER : MCWWS_DEFAULT_CENTER;
        }
    }
}
