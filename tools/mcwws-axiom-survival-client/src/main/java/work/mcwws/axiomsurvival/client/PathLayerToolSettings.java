package work.mcwws.axiomsurvival.client;

/**
 * 钢笔图层的工具级参数（与节点/点配置一起按层隔离）。
 * 数值与 Axiom {@code PathTool} 构造默认值对齐。
 */
public final class PathLayerToolSettings {

    public int curveType = 1;
    public boolean looped;
    public boolean useStairsAndSlabs;
    public int shape;
    public int radius;
    public int endRadius;
    public int depth;
    public boolean inverted;
    public int slack = 20;
    public boolean keepExisting;
    public boolean extendToGround;

    public PathLayerToolSettings copy() {
        PathLayerToolSettings c = new PathLayerToolSettings();
        c.copyFrom(this);
        return c;
    }

    public void copyFrom(PathLayerToolSettings other) {
        if (other == null) {
            return;
        }
        this.curveType = other.curveType;
        this.looped = other.looped;
        this.useStairsAndSlabs = other.useStairsAndSlabs;
        this.shape = other.shape;
        this.radius = other.radius;
        this.endRadius = other.endRadius;
        this.depth = other.depth;
        this.inverted = other.inverted;
        this.slack = other.slack;
        this.keepExisting = other.keepExisting;
        this.extendToGround = other.extendToGround;
    }

    public void appendJson(StringBuilder sb, String indent) {
        sb.append(indent).append("\"settings\": {\n");
        sb.append(indent).append("  \"curveType\": ").append(curveType).append(",\n");
        sb.append(indent).append("  \"looped\": ").append(looped).append(",\n");
        sb.append(indent).append("  \"useStairsAndSlabs\": ").append(useStairsAndSlabs).append(",\n");
        sb.append(indent).append("  \"shape\": ").append(shape).append(",\n");
        sb.append(indent).append("  \"radius\": ").append(radius).append(",\n");
        sb.append(indent).append("  \"endRadius\": ").append(endRadius).append(",\n");
        sb.append(indent).append("  \"depth\": ").append(depth).append(",\n");
        sb.append(indent).append("  \"inverted\": ").append(inverted).append(",\n");
        sb.append(indent).append("  \"slack\": ").append(slack).append(",\n");
        sb.append(indent).append("  \"keepExisting\": ").append(keepExisting).append(",\n");
        sb.append(indent).append("  \"extendToGround\": ").append(extendToGround).append('\n');
        sb.append(indent).append('}');
    }
}
