package work.mcwws.axiomsurvival.client;

import com.moulberry.axiom.tools.path.PointConfig;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 钢笔的一条独立路径（图层）。互不串线，各自栅格化后再合并预览。 */
public final class PathLayer {

    public String name;
    /** false 时不参与方块预览与落块，仍可切换编辑节点。 */
    public boolean visible = true;
    public final List<Vec3> points = new ArrayList<>();
    public final List<PointConfig> configs = new ArrayList<>();

    public PathLayer(String name) {
        this.name = name;
    }

    public PathLayer copy() {
        PathLayer copy = new PathLayer(name);
        copy.visible = visible;
        copy.points.addAll(points);
        for (PointConfig config : configs) {
            copy.configs.add(new PointConfig(config));
        }
        return copy;
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }
}
