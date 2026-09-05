package work.mcwws.axiomsurvival.client;

import imgui.moulberry92.ImDrawList;
import imgui.moulberry92.ImGui;

/**
 * 用 ImDrawList 画睁眼/闭眼与删除叉，避免依赖中文字体字形。
 */
public final class PathLayerIcons {

    private PathLayerIcons() {
    }

    public static boolean eyeToggle(String id, boolean visible, float size) {
        ImGui.pushID(id);
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        boolean clicked = ImGui.invisibleButton("##hit", size, size);
        boolean hovered = ImGui.isItemHovered();
        ImDrawList dl = ImGui.getWindowDrawList();
        int col = ImGui.getColorU32(1f, 1f, 1f, hovered ? 1f : 0.82f);
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        // 自行描点保证横放（水平为长轴），不依赖 addEllipse 参数轴序
        float horizontal = size * 0.38f;
        float vertical = size * 0.20f;
        drawEllipseOutline(dl, cx, cy, horizontal, vertical, col, 1.6f);
        if (visible) {
            dl.addCircleFilled(cx, cy, size * 0.10f, col);
        } else {
            dl.addLine(
                    x + size * 0.18f,
                    y + size * 0.78f,
                    x + size * 0.82f,
                    y + size * 0.22f,
                    col,
                    1.7f
            );
        }
        ImGui.popID();
        return clicked;
    }

    public static boolean deleteButton(String id, float size) {
        ImGui.pushID(id);
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        boolean clicked = ImGui.invisibleButton("##hit", size, size);
        boolean hovered = ImGui.isItemHovered();
        ImDrawList dl = ImGui.getWindowDrawList();
        int col = ImGui.getColorU32(1f, hovered ? 0.35f : 0.55f, hovered ? 0.35f : 0.55f, 1f);
        float pad = size * 0.28f;
        dl.addLine(x + pad, y + pad, x + size - pad, y + size - pad, col, 1.7f);
        dl.addLine(x + size - pad, y + pad, x + pad, y + size - pad, col, 1.7f);
        ImGui.popID();
        return clicked;
    }

    private static void drawEllipseOutline(
            ImDrawList dl,
            float cx,
            float cy,
            float radiusX,
            float radiusY,
            int col,
            float thickness
    ) {
        final int segments = 24;
        dl.pathClear();
        for (int i = 0; i <= segments; i++) {
            double a = (Math.PI * 2.0 * i) / segments;
            dl.pathLineTo(
                    cx + (float) (Math.cos(a) * radiusX),
                    cy + (float) (Math.sin(a) * radiusY)
            );
        }
        dl.pathStroke(col, 0, thickness);
    }
}
