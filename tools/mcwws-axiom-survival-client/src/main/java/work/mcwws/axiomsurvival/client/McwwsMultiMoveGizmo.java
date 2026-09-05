package work.mcwws.axiomsurvival.client;

import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.tools.modelling.GizmoList;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 整组节点一次移动的历史动作：按索引把各点设到对应位置（撤销=起点，重做=终点）。
 */
public final class McwwsMultiMoveGizmo implements GizmoList.GizmoListHistoryAction {

    private final int[] indices;
    private final Vec3[] positions;

    public McwwsMultiMoveGizmo(int[] indices, Vec3[] positions) {
        if (indices.length != positions.length) {
            throw new IllegalArgumentException("indices/positions length mismatch");
        }
        this.indices = indices.clone();
        this.positions = positions.clone();
    }

    @Override
    public void apply(GizmoList list) {
        List<Gizmo> gizmos = list.getGizmos();
        for (int i = 0; i < indices.length; i++) {
            int index = indices[i];
            if (index < 0 || index >= gizmos.size()) {
                continue;
            }
            gizmos.get(index).moveToVecInstantly(positions[i]);
        }
    }
}
