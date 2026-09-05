package work.mcwws.axiomsurvival.client;

import com.moulberry.axiom.utils.AsyncFileDialogs;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 官方钢笔没有保存/导入节点。轨迹以 JSON 落在 {@code config/axiom/mcwws-paths/}。
 * version 1：单层 points；version 2：多层 layers；version 3：每层含 settings。
 */
public final class PathLibrary {

    private static final Pattern POINT = Pattern.compile(
            "\"x\"\\s*:\\s*([-+0-9.eE]+)\\s*,\\s*\"y\"\\s*:\\s*([-+0-9.eE]+)\\s*,\\s*\"z\"\\s*:\\s*([-+0-9.eE]+)"
    );
    private static final Pattern LAYER_NAME = Pattern.compile("\"name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern ACTIVE = Pattern.compile("\"active\"\\s*:\\s*(\\d+)");

    private PathLibrary() {
    }

    public static Path defaultDir() {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("config/axiom/mcwws-paths");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            McwwsAxiomSurvivalClientMod.LOGGER.warn("无法创建钢笔轨迹目录 {}", dir, e);
        }
        return dir;
    }

    public static String toJson(List<Vec3> points) {
        PathLayer layer = new PathLayer("图层 1");
        layer.points.addAll(points);
        return toJsonLayers(List.of(layer), 0);
    }

    public static String toJsonLayers(List<PathLayer> layers, int activeIndex) {
        StringBuilder sb = new StringBuilder(256 + layers.size() * 128);
        sb.append("{\n  \"version\": 3,\n  \"active\": ").append(Math.max(0, activeIndex)).append(",\n  \"layers\": [\n");
        for (int li = 0; li < layers.size(); li++) {
            PathLayer layer = layers.get(li);
            sb.append("    {\n      \"name\": \"").append(escape(layer.name)).append("\",\n");
            sb.append("      \"visible\": ").append(layer.visible).append(",\n");
            layer.settings.appendJson(sb, "      ");
            sb.append(",\n      \"points\": [\n");
            for (int i = 0; i < layer.points.size(); i++) {
                Vec3 p = layer.points.get(i);
                sb.append("        {\"x\": ").append(p.x)
                        .append(", \"y\": ").append(p.y)
                        .append(", \"z\": ").append(p.z)
                        .append('}');
                if (i + 1 < layer.points.size()) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            sb.append("      ]\n    }");
            if (li + 1 < layers.size()) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    public static List<Vec3> fromJson(String json) {
        List<PathLayer> layers = fromJsonLayers(json);
        if (layers.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(layers.get(0).points);
    }

    public static List<PathLayer> fromJsonLayers(String json) {
        List<PathLayer> layers = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return layers;
        }
        if (json.contains("\"layers\"")) {
            List<String> blocks = extractLayerBlocks(json);
            if (!blocks.isEmpty()) {
                int idx = 0;
                for (String block : blocks) {
                    String name = "图层 " + (idx + 1);
                    Matcher nameMatcher = LAYER_NAME.matcher(block);
                    if (nameMatcher.find()) {
                        name = nameMatcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
                    }
                    PathLayer layer = new PathLayer(name);
                    layer.visible = !block.contains("\"visible\": false") && !block.contains("\"visible\":false");
                    parseSettingsInto(block, layer.settings);
                    Matcher points = Pattern.compile("\"points\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(block);
                    if (points.find()) {
                        Matcher m = POINT.matcher(points.group(1));
                        while (m.find()) {
                            layer.points.add(new Vec3(
                                    Double.parseDouble(m.group(1)),
                                    Double.parseDouble(m.group(2)),
                                    Double.parseDouble(m.group(3))
                            ));
                        }
                    }
                    layers.add(layer);
                    idx++;
                }
                return layers;
            }
            // 旧版宽松解析回退
            Matcher nameMatcher = LAYER_NAME.matcher(json);
            Matcher pointBlock = Pattern.compile("\"points\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(json);
            List<String> names = new ArrayList<>();
            while (nameMatcher.find()) {
                names.add(nameMatcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\"));
            }
            int idx = 0;
            while (pointBlock.find()) {
                PathLayer layer = new PathLayer(idx < names.size() ? names.get(idx) : ("图层 " + (idx + 1)));
                Matcher m = POINT.matcher(pointBlock.group(1));
                while (m.find()) {
                    layer.points.add(new Vec3(
                            Double.parseDouble(m.group(1)),
                            Double.parseDouble(m.group(2)),
                            Double.parseDouble(m.group(3))
                    ));
                }
                layers.add(layer);
                idx++;
            }
            return layers;
        }
        PathLayer layer = new PathLayer("图层 1");
        Matcher m = POINT.matcher(json);
        while (m.find()) {
            layer.points.add(new Vec3(
                    Double.parseDouble(m.group(1)),
                    Double.parseDouble(m.group(2)),
                    Double.parseDouble(m.group(3))
            ));
        }
        if (!layer.isEmpty()) {
            layers.add(layer);
        }
        return layers;
    }

    private static List<String> extractLayerBlocks(String json) {
        int layersKey = json.indexOf("\"layers\"");
        if (layersKey < 0) {
            return List.of();
        }
        int arr = json.indexOf('[', layersKey);
        if (arr < 0) {
            return List.of();
        }
        List<String> blocks = new ArrayList<>();
        int i = arr + 1;
        while (i < json.length()) {
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
                i++;
            }
            if (i >= json.length() || json.charAt(i) == ']') {
                break;
            }
            if (json.charAt(i) != '{') {
                i++;
                continue;
            }
            int depth = 0;
            int begin = i;
            for (; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        blocks.add(json.substring(begin, i + 1));
                        i++;
                        break;
                    }
                }
            }
        }
        return blocks;
    }

    private static void parseSettingsInto(String block, PathLayerToolSettings settings) {
        settings.curveType = intField(block, "curveType", settings.curveType);
        settings.looped = boolField(block, "looped", settings.looped);
        settings.useStairsAndSlabs = boolField(block, "useStairsAndSlabs", settings.useStairsAndSlabs);
        settings.shape = intField(block, "shape", settings.shape);
        settings.radius = intField(block, "radius", settings.radius);
        settings.endRadius = intField(block, "endRadius", settings.endRadius);
        settings.depth = intField(block, "depth", settings.depth);
        settings.inverted = boolField(block, "inverted", settings.inverted);
        settings.slack = intField(block, "slack", settings.slack);
        settings.keepExisting = boolField(block, "keepExisting", settings.keepExisting);
        settings.extendToGround = boolField(block, "extendToGround", settings.extendToGround);
    }

    private static int intField(String block, String key, int fallback) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(block);
        return m.find() ? Integer.parseInt(m.group(1)) : fallback;
    }

    private static boolean boolField(String block, String key, boolean fallback) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)").matcher(block);
        return m.find() ? Boolean.parseBoolean(m.group(1)) : fallback;
    }

    public static int activeIndexFromJson(String json) {
        Matcher m = ACTIVE.matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    public static void saveDialog(List<Vec3> points) {
        if (points == null || points.isEmpty() || AsyncFileDialogs.hasDialog()) {
            return;
        }
        saveDialogLayers(List.of(newLayer("图层 1", points)), 0);
    }

    public static void saveDialogLayers(List<PathLayer> layers, int activeIndex) {
        if (layers == null || layers.isEmpty() || AsyncFileDialogs.hasDialog()) {
            return;
        }
        boolean any = false;
        for (PathLayer layer : layers) {
            if (!layer.isEmpty()) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        String json = toJsonLayers(layers, activeIndex);
        AsyncFileDialogs.saveFileDialog(defaultDir().toString(), "path.json", "MCWWS Path", "json")
                .thenAccept(path -> {
                    if (path == null || path.isBlank()) {
                        return;
                    }
                    Minecraft.getInstance().execute(() -> writeFile(path, json));
                });
    }

    public static void openDialog(Consumer<List<Vec3>> onLoad) {
        openDialogLayers((layers, active) -> {
            if (layers.isEmpty()) {
                return;
            }
            onLoad.accept(new ArrayList<>(layers.get(Math.max(0, Math.min(active, layers.size() - 1))).points));
        });
    }

    public static void openDialogLayers(BiConsumer<List<PathLayer>, Integer> onLoad) {
        if (onLoad == null || AsyncFileDialogs.hasDialog()) {
            return;
        }
        AsyncFileDialogs.openFileDialog(defaultDir().toString(), "MCWWS Path", "json")
                .thenAccept(path -> {
                    if (path == null || path.isBlank()) {
                        return;
                    }
                    Minecraft.getInstance().execute(() -> {
                        try {
                            String json = Files.readString(Path.of(path), StandardCharsets.UTF_8);
                            List<PathLayer> layers = fromJsonLayers(json);
                            if (layers.isEmpty()) {
                                McwwsAxiomSurvivalClientMod.LOGGER.warn("钢笔轨迹文件没有节点: {}", path);
                                return;
                            }
                            onLoad.accept(layers, activeIndexFromJson(json));
                        } catch (Exception e) {
                            McwwsAxiomSurvivalClientMod.LOGGER.warn("读取钢笔轨迹失败: {}", path, e);
                        }
                    });
                });
    }

    private static PathLayer newLayer(String name, List<Vec3> points) {
        PathLayer layer = new PathLayer(name);
        layer.points.addAll(points);
        return layer;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void writeFile(String path, String json) {
        try {
            Path file = Path.of(path);
            if (!file.getFileName().toString().contains(".")) {
                file = Path.of(path + ".json");
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, json, StandardCharsets.UTF_8);
            McwwsAxiomSurvivalClientMod.LOGGER.info("已保存钢笔轨迹 -> {}", file);
        } catch (Exception e) {
            McwwsAxiomSurvivalClientMod.LOGGER.warn("保存钢笔轨迹失败: {}", path, e);
        }
    }
}
