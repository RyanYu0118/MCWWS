package work.mcwws.axiomsurvival.client;

import com.moulberry.axiom.utils.AsyncFileDialogs;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 官方钢笔没有保存/导入节点。轨迹以 JSON 落在 {@code config/axiom/mcwws-paths/}。
 */
public final class PathLibrary {

    private static final Pattern POINT = Pattern.compile(
            "\"x\"\\s*:\\s*([-+0-9.eE]+)\\s*,\\s*\"y\"\\s*:\\s*([-+0-9.eE]+)\\s*,\\s*\"z\"\\s*:\\s*([-+0-9.eE]+)"
    );

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
        StringBuilder sb = new StringBuilder(64 + points.size() * 48);
        sb.append("{\n  \"version\": 1,\n  \"points\": [\n");
        for (int i = 0; i < points.size(); i++) {
            Vec3 p = points.get(i);
            sb.append("    {\"x\": ").append(p.x)
                    .append(", \"y\": ").append(p.y)
                    .append(", \"z\": ").append(p.z)
                    .append('}');
            if (i + 1 < points.size()) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    public static List<Vec3> fromJson(String json) {
        List<Vec3> out = new ArrayList<>();
        Matcher m = POINT.matcher(json);
        while (m.find()) {
            out.add(new Vec3(
                    Double.parseDouble(m.group(1)),
                    Double.parseDouble(m.group(2)),
                    Double.parseDouble(m.group(3))
            ));
        }
        return out;
    }

    public static void saveDialog(List<Vec3> points) {
        if (points == null || points.isEmpty() || AsyncFileDialogs.hasDialog()) {
            return;
        }
        String json = toJson(points);
        AsyncFileDialogs.saveFileDialog(defaultDir().toString(), "path.json", "MCWWS Path", "json")
                .thenAccept(path -> {
                    if (path == null || path.isBlank()) {
                        return;
                    }
                    Minecraft.getInstance().execute(() -> writeFile(path, json));
                });
    }

    public static void openDialog(Consumer<List<Vec3>> onLoad) {
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
                            List<Vec3> points = fromJson(json);
                            if (points.isEmpty()) {
                                McwwsAxiomSurvivalClientMod.LOGGER.warn("钢笔轨迹文件没有节点: {}", path);
                                return;
                            }
                            onLoad.accept(points);
                        } catch (Exception e) {
                            McwwsAxiomSurvivalClientMod.LOGGER.warn("读取钢笔轨迹失败: {}", path, e);
                        }
                    });
                });
    }

    private static void writeFile(String path, String json) {
        try {
            Path file = Path.of(path);
            if (!file.getFileName().toString().contains(".")) {
                file = Path.of(path + ".json");
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, json, StandardCharsets.UTF_8);
            McwwsAxiomSurvivalClientMod.LOGGER.info("已保存钢笔轨迹 {} 个节点 -> {}", jsonPointCount(json), file);
        } catch (Exception e) {
            McwwsAxiomSurvivalClientMod.LOGGER.warn("保存钢笔轨迹失败: {}", path, e);
        }
    }

    private static int jsonPointCount(String json) {
        return fromJson(json).size();
    }
}
