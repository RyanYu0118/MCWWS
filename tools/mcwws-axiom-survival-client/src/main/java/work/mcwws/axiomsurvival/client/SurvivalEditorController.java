package work.mcwws.axiomsurvival.client;

import com.moulberry.axiom.editor.EditorUI;
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

    private static boolean hasMenuPose;
    private static double menuPosX;
    private static double menuPosY;
    private static double menuPosZ;
    private static float menuYaw;
    private static float menuPitch;

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
        return localEditorActive && !EditorUI.isEnabled();
    }

    /** 打开 Editor 菜单（切旁观）前记录建造位置 */
    public static void captureMenuOpenPose() {
        if (!localEditorActive) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        var player = mc.player;
        menuPosX = player.getX();
        menuPosY = player.getY();
        menuPosZ = player.getZ();
        menuYaw = player.getYRot();
        menuPitch = player.getXRot();
        hasMenuPose = true;
    }

    /**
     * 关菜单后：强制旁观→创造、相机回玩家、恢复开菜单前坐标。
     * {@code setLocalMode}  alone 不会复位旁观相机，且服务端生存包会覆盖本地创造。
     */
    public static void finalizeMenuClose() {
        if (!localEditorActive || EditorUI.isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            return;
        }
        var player = mc.player;
        mc.gameMode.setLocalMode(GameType.CREATIVE);
        mc.setCameraEntity(player);
        player.removeVehicle();
        if (hasMenuPose) {
            player.teleportTo(menuPosX, menuPosY, menuPosZ);
            player.setYRot(menuYaw);
            player.setXRot(menuPitch);
            hasMenuPose = false;
        }
        mc.gameMode.adjustPlayer(player);
        onEnterBuildMode();
        McwwsAxiomSurvivalClientMod.LOGGER.debug("Editor 菜单已关闭：已恢复建造模式与位置");
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
        GameType restore = storedMode;
        localEditorActive = false;
        storedMode = null;
        hasMenuPose = false;
        SurvivalEditorNetworking.sendEditorState(false);
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        if (mc.gameMode != null && restore != null) {
            mc.gameMode.setLocalMode(restore);
        }
        McwwsAxiomSurvivalClientMod.LOGGER.info("生存 Editor 会话结束");
    }
}
