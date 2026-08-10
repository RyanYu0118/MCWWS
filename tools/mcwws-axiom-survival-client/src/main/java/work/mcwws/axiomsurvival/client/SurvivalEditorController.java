package work.mcwws.axiomsurvival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;

/**
 * 客户端生存 Editor 状态：服务端保持生存，本地 MultiPlayerGameMode 切到旁观以驱动 Axiom UI。
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

    public static void onEditorEnter() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            return;
        }
        if (localEditorActive) {
            return;
        }
        storedMode = mc.gameMode.getPlayerMode();
        mc.gameMode.setLocalMode(GameType.SPECTATOR);
        localEditorActive = true;
        SurvivalEditorNetworking.sendEditorState(true);
        McwwsAxiomSurvivalClientMod.LOGGER.info("生存 Editor 进入（本地旁观）");
    }

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
        McwwsAxiomSurvivalClientMod.LOGGER.info("生存 Editor 退出");
    }
}
