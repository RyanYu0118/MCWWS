package work.mcwws.axiomsurvival.client;

import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.integration.ServerIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;

/**
 * Axiom Editor 菜单：{@code enable()} → 旁观；{@code disable()} → 创造（仍处 Editor，非退出）。
 * 完全退出 Editor 才会切回生存/冒险。本地模式由 Axiom {@code changeGameMode} 设置，此处只跟踪会话。
 */
public final class SurvivalEditorController {

    private static volatile boolean serverSupported;
    private static volatile boolean entityGizmoAllowed;
    private static volatile boolean localEditorActive;
    private static GameType storedMode;

    private static boolean hasMenuPose;
    private static double menuPosX;
    private static double menuPosY;
    private static double menuPosZ;
    private static float menuYaw;
    private static float menuPitch;
    private static boolean menuFlying;

    /** 服务端下发的 abilities 是唯一权威；本地创造会污染 mayfly/instabuild，需要这份原值压回去 */
    private static volatile boolean hasServerAbilities;
    private static volatile boolean serverMayfly;
    private static volatile boolean serverFlying;
    private static volatile boolean serverInstabuild;
    private static volatile boolean serverInvulnerable;

    /**
     * 关菜单前记下的 NMS {@code flyingSpeed}。Axiom 若开启「编辑界面与游戏内分开记速度」，
     * {@code EditorUI.disable()} 会把它换回进菜单前的值，必须在那之前捕获。
     */
    private static float pendingEditorFlySpeed = Float.NaN;
    private static float lastPersistedFlySpeed = Float.NaN;

    private SurvivalEditorController() {
    }

    /** vanilla / Axiom 使用的 NMS 飞行速度，默认 0.05 */
    public static float currentNmsFlySpeed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0.05f;
        }
        return mc.player.getAbilities().getFlyingSpeed();
    }

    /** {@code EditorUI.disable()} 开头调用：此时 abilities 仍是编辑界面里刚调过的速度 */
    public static void captureEditorFlySpeedBeforeMenuClose() {
        if (!EditorUI.isEnabled()) {
            return;
        }
        pendingEditorFlySpeed = currentNmsFlySpeed();
        McwwsAxiomSurvivalClientMod.LOGGER.debug(
                "关菜单前捕获飞行速度: {}", pendingEditorFlySpeed
        );
    }

    private static float takePersistedFlySpeed() {
        float captured = pendingEditorFlySpeed;
        pendingEditorFlySpeed = Float.NaN;
        if (Float.isFinite(captured) && captured > 0f) {
            return captured;
        }
        return currentNmsFlySpeed();
    }

    public static void clearPersistedFlySpeed() {
        pendingEditorFlySpeed = Float.NaN;
        lastPersistedFlySpeed = Float.NaN;
    }

    private static void persistFlySpeedLocally(float nmsSpeed) {
        if (!Float.isFinite(nmsSpeed) || nmsSpeed <= 0f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.getAbilities().setFlyingSpeed(nmsSpeed);
        lastPersistedFlySpeed = nmsSpeed;
        ServerIntegration.changeFlySpeed(nmsSpeed);
    }

    public static boolean isServerSupported() {
        return serverSupported;
    }

    public static void setServerSupported(boolean supported) {
        serverSupported = supported;
        if (!supported) {
            entityGizmoAllowed = false;
        }
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

    /**
     * 服务端始终是生存：挖掘进度、进食等不要走本地创造的 instabuild/无敌捷径。
     */
    public static boolean shouldActLikeSurvival() {
        return serverSupported && localEditorActive;
    }

    /**
     * 服务端始终是生存，本地创造的 {@code instabuild} 秒破路径不会被服务端认可，
     * 因此会话内手动挖掘必须走生存进度逻辑。
     */
    public static boolean shouldMineLikeSurvival() {
        return shouldActLikeSurvival();
    }

    /**
     * {@code AxiomClient.isAxiomActive} 要求本地模式恰好等于创造（菜单开启时为旁观），
     * 否则工具槽与全部建筑工具都不可用。未进 Editor 时本地模式是真生存，
     * 因此只对该判定谎报虚拟模式，本地模式与背包/飞行/挖掘仍保持真生存。
     */
    public static boolean shouldSpoofAxiomActive() {
        return serverSupported;
    }

    /**
     * 生存里 {@code isAxiomActive} 被谎报为真后，Axiom 会给附近展示实体画出可拖动小方块，
     * 容易误触村民/展示框/盔甲架。默认关掉；只有服务端授予实体操纵权限才显示。
     * 未接到本服 hello 时不干预（单人/其他服仍走 Axiom 原逻辑）。
     */
    public static boolean shouldShowEntityGizmos() {
        return !serverSupported || entityGizmoAllowed;
    }

    public static void setEntityGizmoAllowed(boolean allowed) {
        if (entityGizmoAllowed == allowed) {
            return;
        }
        entityGizmoAllowed = allowed;
        McwwsAxiomSurvivalClientMod.LOGGER.info(
                allowed ? "已允许显示实体操纵小方块" : "已关闭实体操纵小方块"
        );
    }

    /** 供 Axiom 激活判定使用的虚拟模式：菜单开启时旁观，否则创造 */
    public static GameType virtualAxiomMode() {
        return EditorUI.isEnabled() ? GameType.SPECTATOR : GameType.CREATIVE;
    }

    /** 打开 Editor 菜单（切旁观）前记录建造位置与飞行状态，并请求服务端快照 */
    public static void onMenuOpening() {
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
        menuFlying = player.getAbilities().flying;
        hasMenuPose = true;
        McwwsAxiomSurvivalClientMod.LOGGER.debug("开菜单快照: flying={}", menuFlying);
        SurvivalEditorNetworking.sendMenuState(true, menuFlying);
    }

    /**
     * 关菜单后：相机回玩家、恢复开菜单前坐标与飞行状态。客户端传送会被服务端移动校验拉回，
     * 因此同时请求服务端权威传送；旁观会强制 {@code flying}，切回创造时 vanilla 不会复位。
     * 飞行状态只本地先行改一次做即时反馈，权威值由服务端 abilities 包下发。
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
        float flySpeed = takePersistedFlySpeed();
        persistFlySpeedLocally(flySpeed);
        SurvivalEditorNetworking.sendMenuState(false, menuFlying, flySpeed);
        mc.gameMode.setLocalMode(GameType.CREATIVE);
        mc.setCameraEntity(player);
        if (hasMenuPose) {
            player.teleportTo(menuPosX, menuPosY, menuPosZ);
            player.setYRot(menuYaw);
            player.setXRot(menuPitch);
            player.resetFallDistance();
            hasMenuPose = false;
        }
        clampAbilitiesToServerState();
        onEnterBuildMode();
        McwwsAxiomSurvivalClientMod.LOGGER.debug(
                "关菜单: 已恢复建造模式与位置，abilities 按服务端 mayfly={} flying={} instabuild={}",
                serverMayfly, serverFlying, serverInstabuild
        );
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

    /** 服务端每次下发 abilities 都记一份，这是玩家真实能力的唯一依据 */
    public static void noteServerAbilities(
            boolean mayfly, boolean flying, boolean instabuild, boolean invulnerable
    ) {
        serverMayfly = mayfly;
        serverFlying = flying;
        serverInstabuild = instabuild;
        serverInvulnerable = invulnerable;
        hasServerAbilities = true;
        // 建造阶段若刚收到服务端包（例如 /fly），vanilla 已写入正确值；
        // 但本地创造可能在同 tick 稍后再次污染，下一 tick 的 setLocalMode 兜底会再压一次
        clampAbilitiesToServerState();
    }

    /**
     * 本地模式切到创造后 vanilla 会把 {@code mayfly} 置 true，凭空给飞行。
     * 只压飞行；{@code instabuild} 必须留给 Axiom 工具菜单——它每 tick 检查
     * {@code hasInfiniteMaterials()}，为 false 会立刻关掉 SwitchHotbarScreen。
     * E 键创造物品栏由 {@code InventoryScreen} mixin 单独拦截。
     */
    public static void clampAbilitiesToServerState() {
        if (!localEditorActive || EditorUI.isEnabled()) {
            return;
        }
        applyServerFlight();
    }

    /** {@code setLocalMode} 之后的兜底：创造即建造阶段，撤掉 vanilla 附带的飞行权限 */
    public static void afterLocalModeChanged(GameType gameType) {
        if (gameType == GameType.CREATIVE) {
            clampAbilitiesToServerState();
        }
    }

    private static void applyServerFlight() {
        if (!hasServerAbilities) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        var abilities = mc.player.getAbilities();
        abilities.mayfly = serverMayfly;
        abilities.flying = serverMayfly && serverFlying;
        if (Float.isFinite(lastPersistedFlySpeed) && lastPersistedFlySpeed > 0f) {
            abilities.setFlyingSpeed(lastPersistedFlySpeed);
        }
    }

    private static void applyServerAbilities() {
        applyServerFlight();
        if (!hasServerAbilities) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        var abilities = mc.player.getAbilities();
        abilities.instabuild = serverInstabuild;
        abilities.invulnerable = serverInvulnerable;
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
        // 本局还没收到过 abilities 包时（理论上加入世界就会收到），用进 Editor 前的现值兜底
        if (!hasServerAbilities && mc.player != null) {
            var abilities = mc.player.getAbilities();
            noteServerAbilities(
                    abilities.mayfly, abilities.flying, abilities.instabuild, abilities.invulnerable
            );
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
        if (gameType == GameType.CREATIVE) {
            clampAbilitiesToServerState();
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
        float flySpeed = takePersistedFlySpeed();
        persistFlySpeedLocally(flySpeed);
        SurvivalEditorNetworking.sendEditorState(false, flySpeed);
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        if (mc.gameMode != null && restore != null) {
            mc.gameMode.setLocalMode(restore);
        }
        // 切回生存的 updatePlayerAbilities 会一并清掉飞行，玩家自己开的飞行要按服务端值恢复
        applyServerAbilities();
        McwwsAxiomSurvivalClientMod.LOGGER.info("生存 Editor 会话结束");
    }
}
