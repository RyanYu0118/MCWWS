package work.mcwws.axiomsurvival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;

/**
 * Axiom Editor 菜单：{@code enable()} → 旁观；{@code disable()} → 创造（仍处 Editor，非退出）。
 * 完全退出 Editor 才会切回生存/冒险。本地模式由 Axiom {@code changeGameMode} 设置，此处只跟踪会话。
 */
public final class SurvivalEditorController {

    private static volatile boolean serverSupported;
    private static volatile boolean localEditorActive;
    private static GameType storedMode;

    private SurvivalEditorController() {
    }

    public static boolean isServerSupported() {
        return serverSupported;
    }

    public static void setServerSupported(boolean supported) {
        serverSupported = supported;
        McwwsAxiomSurvivalClientMod.LOGGER.info(
                supported ? "服务端已宣告生存 Editor 支持" : "服务端生存 Editor 支持已关闭"
        );
    }

    public static void markSupportedFromHello() {
        setServerSupported(true);
    }

    public static boolean isLocalEditorActive() {
        return localEditorActive;
    }

    public static boolean shouldSuppressGamemodeSync() {
        return serverSupported && localEditorActive;
    }

    /** Editor 建造阶段（菜单关闭）应显示生存 HUD，含经验条 */
    public static boolean shouldShowSurvivalHud() {
        return localEditorActive && !com.moulberry.axiom.editor.EditorUI.isEnabled();
    }

    /** 关闭 Editor 菜单、回到建造时刷新经验条显示优先级 */
    public static void onEnterBuildMode() {
        if (!localEditorActive) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        try {
            var player = mc.player;
            int tickCount = player.getClass().getField("tickCount").getInt(player);
            player.getClass().getField("experienceDisplayStartTick").setInt(player, tickCount);
        } catch (ReflectiveOperationException e) {
            McwwsAxiomSurvivalClientMod.LOGGER.warn("无法刷新经验条显示计时", e);
        }
    }

    /** 在 Axiom {@code changeGameMode} 执行前捕获真实生存模式 */
    public static void captureStoredModeBeforeEnter() {
        if (localEditorActive) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            storedMode = mc.gameMode.getPlayerMode();
        }
    }

    /** Axiom 请求旁观或创造 — 开始/维持 Editor 会话 */
    public static void onEditorGamemodeChange(GameType gameType) {
        if (gameType != GameType.SPECTATOR && gameType != GameType.CREATIVE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            return;
        }
        if (!localEditorActive) {
            if (storedMode == null) {
                storedMode = mc.gameMode.getPlayerMode();
            }
            localEditorActive = true;
            SurvivalEditorNetworking.sendEditorState(true);
            McwwsAxiomSurvivalClientMod.LOGGER.info("生存 Editor 会话开始（本地 {}）", gameType.getName());
        }
    }

    /** 完全退出 Axiom Editor */
    public static void onEditorExit() {
        if (!localEditorActive) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null && storedMode != null) {
            mc.gameMode.setLocalMode(storedMode);
        }
        localEditorActive = false;
        storedMode = null;
        SurvivalEditorNetworking.sendEditorState(false);
        McwwsAxiomSurvivalClientMod.LOGGER.info("生存 Editor 会话结束");
    }
}
