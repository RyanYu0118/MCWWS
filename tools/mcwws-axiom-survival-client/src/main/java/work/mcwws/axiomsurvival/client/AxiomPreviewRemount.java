package work.mcwws.axiomsurvival.client;

import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.hooks.WorldRenderHook;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.utils.RenderHelper;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;

import java.lang.reflect.Method;

/**
 * Vitrail 光影包会在 {@code LevelRenderer.render} 之后继续合成，把 Axiom 官方挂在
 * RETURN 上的粘贴/蓝图幽灵覆盖掉。光影激活时在更晚的时机再跑一遍
 * {@link WorldRenderHook}。无 Vitrail 或未在画包时不重挂，避免双影。
 */
public final class AxiomPreviewRemount {

    private static Boolean packApi;
    private static Method drawingPack;
    private static boolean remountedThisFrame;

    private AxiomPreviewRemount() {
    }

    public static boolean vitrailDrawingPack() {
        if (packApi == Boolean.FALSE) {
            return false;
        }
        try {
            if (drawingPack == null) {
                Class<?> packChain = Class.forName("dev.vitrail.render.PackChain");
                drawingPack = packChain.getMethod("drawingPack");
                packApi = Boolean.TRUE;
            }
            Object value = drawingPack.invoke(null);
            return value instanceof Boolean && (Boolean) value;
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            packApi = Boolean.FALSE;
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void beginFrame() {
        remountedThisFrame = false;
    }

    public static void remountOncePerTick(GameRenderer renderer) {
        if (remountedThisFrame || !vitrailDrawingPack() || !AxiomClient.isAxiomActive()) {
            return;
        }
        LevelRenderState levelState = renderer.gameRenderState().levelRenderState;
        if (levelState == null) {
            return;
        }
        CameraRenderState camera = levelState.cameraRenderState;
        if (camera == null) {
            return;
        }
        AxiomWorldRenderContext context = new AxiomWorldRenderContext(levelState.worldPartialTicks, camera);
        try {
            RenderHelper.legacyEndRenderBufferBatch();
            RenderHelper.pushModelViewStackWithIdentity();
            WorldRenderHook.renderPre(context);
            WorldRenderHook.renderPost(context);
            RenderHelper.legacyEndRenderBufferBatch();
            remountedThisFrame = true;
        } finally {
            try {
                RenderHelper.popModelViewStack();
            } catch (Throwable ignored) {
                // 栈未成功 push 时不要让一帧渲染崩掉
            }
        }
    }
}
