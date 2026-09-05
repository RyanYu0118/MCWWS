package work.mcwws.axiomsurvival.client;

import java.util.List;

/** {@code PathTool} 上的多路径图层入口（由 mixin 实现）。 */
public interface McwwsPathLayers {
    List<PathLayer> mcwwsAllLayers();

    int mcwwsActiveLayerIndex();

    void mcwwsReplaceAllLayers(List<PathLayer> layers, int activeIndex);
}
