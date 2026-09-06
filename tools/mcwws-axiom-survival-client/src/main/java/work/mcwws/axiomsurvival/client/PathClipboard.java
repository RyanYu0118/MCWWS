package work.mcwws.axiomsurvival.client;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.tools.path.PointConfig;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 钢笔图层剪贴板（不是 Axiom 方块剪贴板）。含节点、工具参数与选块。 */
public final class PathClipboard {

    public final List<Vec3> points = new ArrayList<>();
    public final List<PointConfig> configs = new ArrayList<>();
    public final PathLayerToolSettings settings = new PathLayerToolSettings();
    public CustomBlockState block = PathLayerBlocks.defaultBlock();

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public void clear() {
        points.clear();
        configs.clear();
        settings.copyFrom(new PathLayerToolSettings());
        block = PathLayerBlocks.defaultBlock();
    }

    public void set(
            List<Vec3> pts,
            List<PointConfig> cfgs,
            PathLayerToolSettings toolSettings,
            CustomBlockState layerBlock
    ) {
        clear();
        points.addAll(pts);
        for (PointConfig config : cfgs) {
            configs.add(new PointConfig(config));
        }
        if (toolSettings != null) {
            settings.copyFrom(toolSettings);
        }
        if (layerBlock != null) {
            block = layerBlock;
        }
    }

    public PathLayer toLayer(String name) {
        PathLayer layer = new PathLayer(name);
        layer.points.addAll(points);
        for (PointConfig config : configs) {
            layer.configs.add(new PointConfig(config));
        }
        layer.settings.copyFrom(settings);
        layer.block = block;
        return layer;
    }
}
