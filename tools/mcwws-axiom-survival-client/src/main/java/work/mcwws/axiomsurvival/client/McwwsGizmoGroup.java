package work.mcwws.axiomsurvival.client;

import java.util.List;

/** {@code GizmoList} 上的全选入口（由 mixin 实现）。 */
public interface McwwsGizmoGroup {
    void mcwwsSelectAll();

    /** 当前金色多选下标，从高到低；单选或未成组时为空。 */
    List<Integer> mcwwsSelectedDescending();
}
