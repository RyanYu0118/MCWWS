package work.mcwws.axiomsurvival.client;

import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.blueprint.Blueprint;
import com.moulberry.axiom.blueprint.BlueprintIo;
import com.moulberry.axiom.clipboard.Clipboard;
import com.moulberry.axiom.clipboard.ClipboardObject;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.schematic.SchematicLoader;
import com.moulberry.axiom.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallbackI;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 从资源管理器把投影文件拖进游戏窗口，直接进入 Axiom 的放置预览。
 *
 * <p>省去「打开投影浏览器 → 导入 → 选中 → 粘贴」：落点与 Ctrl+V 完全一致，
 * 由 {@link Clipboard} 自己按准星命中面计算，再交给 Axiom 的 Placement 跟随鼠标。
 * 落块仍走正常改块包，服务端 MCWWS_AxiomSurvival 照常计费。
 *
 * <p>Axiom Editor 是 ImGui 叠加层而非 {@code Screen}，收不到 vanilla 的
 * {@code onFilesDrop}，因此这里直接挂 GLFW 回调；Editor 未激活时原样转发给
 * vanilla 之前注册的回调，不影响拖资源包等原有行为。
 */
public final class SchematicDropHandler {

    private static boolean installed;
    private static GLFWDropCallbackI previous;

    private SchematicDropHandler() {
    }

    /** 窗口创建后调用；重复调用无副作用 */
    public static void install() {
        if (installed) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return;
        }
        long handle = mc.getWindow().handle();
        if (handle == 0L) {
            return;
        }
        previous = GLFW.glfwSetDropCallback(handle, SchematicDropHandler::onFilesDropped);
        installed = true;
        McwwsAxiomSurvivalClientMod.LOGGER.info("已接管窗口拖放：Editor 内可直接拖入投影文件");
    }

    private static void onFilesDropped(long window, int count, long names) {
        Path schematic = EditorUI.isActive() ? firstSupported(count, names) : null;
        if (schematic == null) {
            forward(window, count, names);
            return;
        }
        // GLFW 回调运行在事件轮询里，落块与渲染状态一律回主线程再动
        Minecraft.getInstance().execute(() -> pasteIntoWorld(schematic));
    }

    private static Path firstSupported(int count, long names) {
        for (int i = 0; i < count; i++) {
            String raw;
            try {
                raw = org.lwjgl.glfw.GLFWDropCallback.getName(names, i);
            } catch (RuntimeException ignored) {
                continue;
            }
            if (raw == null || raw.isBlank()) {
                continue;
            }
            Path path = Path.of(raw);
            if (isSupported(path) && Files.isRegularFile(path)) {
                return path;
            }
        }
        return null;
    }

    private static boolean isSupported(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".litematic")
                || name.endsWith(".schem")
                || name.endsWith(".schematic")
                || name.endsWith(".bp");
    }

    private static void forward(long window, int count, long names) {
        if (previous != null) {
            previous.invoke(window, count, names);
        }
    }

    private static void pasteIntoWorld(Path path) {
        String fileName = path.getFileName().toString();
        if (!EditorUI.isActive()) {
            return;
        }
        try {
            if (!loadIntoClipboard(path)) {
                ChatUtils.error("无法识别的投影文件：" + fileName);
                return;
            }
        } catch (SchematicLoader.SchematicLoadException e) {
            ChatUtils.error("投影解析失败：" + fileName + " — " + e.getMessage());
            return;
        } catch (Exception e) {
            McwwsAxiomSurvivalClientMod.LOGGER.warn("读取拖入的投影失败: {}", path, e);
            ChatUtils.error("读取投影失败：" + fileName);
            return;
        }

        UserAction.ActionResult result = Clipboard.INSTANCE.callAction(UserAction.PASTE, null);
        if (result == UserAction.ActionResult.NOT_HANDLED) {
            ChatUtils.warning("已载入 " + fileName + "，请把准星对准要放置的方块后按 Ctrl+V。");
            return;
        }
        ChatUtils.info("已载入 " + fileName + "，移动鼠标定位后左键确认落块。");
    }

    /** @return 是否成功写入 Axiom 剪贴板 */
    private static boolean loadIntoClipboard(Path path) throws Exception {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);

        if (name.endsWith(".bp")) {
            try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
                Blueprint blueprint = BlueprintIo.readBlueprint(in);
                if (blueprint == null) {
                    return false;
                }
                Clipboard.INSTANCE.setClipboard(blueprint);
                return true;
            }
        }

        CompoundTag tag = readNbt(path);
        ClipboardObject object;
        if (name.endsWith(".litematic")) {
            object = SchematicLoader.loadLitematic(tag);
        } else if (name.endsWith(".schem")) {
            object = SchematicLoader.loadSponge(tag);
        } else if (name.endsWith(".schematic")) {
            object = SchematicLoader.loadLegacy(tag);
        } else {
            return false;
        }
        if (object == null) {
            return false;
        }
        Clipboard.INSTANCE.setClipboard(object);
        return true;
    }

    private static CompoundTag readNbt(Path path) throws IOException {
        try {
            return NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
        } catch (IOException notGzipped) {
            return NbtIo.read(path);
        }
    }
}
