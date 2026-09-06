package work.mcwws.axiomsurvival.client;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.tools.path.PointConfig;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 钢笔的一条独立路径（图层）。节点、点配置、工具参数与选块均按层隔离。 */
public final class PathLayer {

    public String name;
    /** false 时不参与方块预览与落块，仍可切换编辑节点。 */
    public boolean visible = true;
    public final List<Vec3> points = new ArrayList<>();
    public final List<PointConfig> configs = new ArrayList<>();
    public final PathLayerToolSettings settings = new PathLayerToolSettings();
    /** 该层默认落块材质（对应 Editor 当前选块）。 */
    public CustomBlockState block = PathLayerBlocks.defaultBlock();

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
        copy.settings.copyFrom(settings);
        copy.block = block;
        return copy;
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }
}
