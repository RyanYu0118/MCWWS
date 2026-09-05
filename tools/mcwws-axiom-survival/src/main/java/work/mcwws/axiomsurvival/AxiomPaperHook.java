package work.mcwws.axiomsurvival;

import com.moulberry.axiom.AxiomPaper;
import com.moulberry.axiom.packet.PacketHandler;
import com.moulberry.axiom.packet.WrapperPacketListener;
import com.moulberry.axiom.packet.impl.DeleteEntityPacketListener;
import com.moulberry.axiom.packet.impl.ManipulateEntityPacketListener;
import com.moulberry.axiom.packet.impl.SetBlockBufferPacketListener;
import com.moulberry.axiom.packet.impl.SetBlockPacketListener;
import com.moulberry.axiom.packet.impl.SetGamemodePacketListener;
import com.moulberry.axiom.packet.impl.SetNoPhysicalTriggerPacketListener;
import com.moulberry.axiom.packet.impl.SetTimePacketListener;
import com.moulberry.axiom.packet.impl.SetWorldPropertyListener;
import com.moulberry.axiom.packet.impl.SpawnEntityPacketListener;
import com.moulberry.axiom.packet.impl.TeleportPacketListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;

public final class AxiomPaperHook {

    private final McwwsAxiomSurvivalPlugin plugin;
    private final ChargeService chargeService;
    private final EditorRestoreService editorRestoreService;
    private final SurvivalEditorService survivalEditorService;
    private final PacketFeeEstimator estimator;
    private boolean installed;

    public AxiomPaperHook(
            McwwsAxiomSurvivalPlugin plugin,
            ChargeService chargeService,
            EditorRestoreService editorRestoreService,
            SurvivalEditorService survivalEditorService
    ) {
        this.plugin = plugin;
        this.chargeService = chargeService;
        this.editorRestoreService = editorRestoreService;
        this.survivalEditorService = survivalEditorService;
        this.estimator = new PacketFeeEstimator(plugin);
    }

    public boolean install() {
        if (installed) {
            return true;
        }
        Plugin axiomPlugin = plugin.getServer().getPluginManager().getPlugin("AxiomPaper");
        if (!(axiomPlugin instanceof AxiomPaper axiomPaper)) {
            return false;
        }
        try {
            AxiomLargePayloadHook.HandlerTable handlerTable = findHandlerTable(axiomPaper);
            Map<String, String> paths = new LinkedHashMap<>();

            hookCharging(axiomPaper, handlerTable, paths, "set_block", true,
                    SetBlockPacketListener::new,
                    (player, buf) -> chargeService.evaluate(
                            player, "set_block", estimator.estimateSetBlockPacket(player, buf))
            );
            hookCharging(axiomPaper, handlerTable, paths, "set_buffer", true,
                    SetBlockBufferPacketListener::new,
                    (player, buf) -> {
                        PacketFeeEstimator.BufferEstimate estimate =
                                estimator.estimateSetBufferPacket(player, buf);
                        if (estimate.type() == 1) {
                            return chargeService.evaluateBiome(
                                    player, "set_buffer_biome", estimate.biomeCells(), estimate.biomeMinDistance());
                        }
                        return chargeService.evaluate(player, "set_buffer", estimate.blocks());
                    }
            );
            // 以下通道在 Axiom 改版时更容易改名，单独兜底，避免连带影响方块扣费
            hookCharging(axiomPaper, handlerTable, paths, "spawn_entity", false,
                    SpawnEntityPacketListener::new,
                    (player, buf) -> chargeService.evaluateEntities(
                            player, "spawn_entity", "生成实体", estimator.countCollection(buf),
                            FeeAccumulator.UNKNOWN_DISTANCE)
            );
            hookCharging(axiomPaper, handlerTable, paths, "delete_entity", false,
                    DeleteEntityPacketListener::new,
                    (player, buf) -> chargeService.evaluateEntities(
                            player, "delete_entity", "删除实体", estimator.countCollection(buf),
                            estimator.minDistanceFromEntityUuids(player, buf))
            );
            hookCharging(axiomPaper, handlerTable, paths, "manipulate_entity", false,
                    ManipulateEntityPacketListener::new,
                    (player, buf) -> chargeService.evaluateEntities(
                            player, "manipulate_entity", "调整实体", estimator.countCollection(buf),
                            estimator.minDistanceFromEntityUuids(player, buf))
            );
            hookCharging(axiomPaper, handlerTable, paths, "set_world_time", false,
                    SetTimePacketListener::new,
                    (player, buf) -> chargeService.evaluateWorldControl(
                            player, "world-control.block-world-time", "world-time-blocked")
            );
            hookCharging(axiomPaper, handlerTable, paths, "set_world_property", false,
                    SetWorldPropertyListener::new,
                    (player, buf) -> chargeService.evaluateWorldControl(
                            player, "world-control.block-world-property", "world-property-blocked")
            );
            replaceChannel(axiomPaper, handlerTable, "set_gamemode", axiom -> {
                PacketHandler original = new SetGamemodePacketListener(axiom);
                return EditorPacketHandlers.wrapGamemode(plugin, editorRestoreService, survivalEditorService, original);
            }, null);
            replaceChannel(axiomPaper, handlerTable, "teleport", axiom -> {
                PacketHandler original = new TeleportPacketListener(axiom);
                return EditorPacketHandlers.wrapTeleport(plugin, editorRestoreService, survivalEditorService, original);
            }, null);
            replaceChannel(axiomPaper, handlerTable, "set_no_physical_trigger", axiom -> {
                PacketHandler original = new SetNoPhysicalTriggerPacketListener(axiom);
                return EditorPacketHandlers.wrapNoPhysicalTrigger(plugin, editorRestoreService, survivalEditorService, original);
            }, null);
            installed = true;
            plugin.getLogger().info("已挂钩 AxiomPaper 扣费通道: " + describePaths(paths)
                    + "；Editor 恢复（含生存 Editor 通道）已就绪。");
            if (!paths.containsKey("set_buffer")) {
                plugin.getLogger().warning(
                        "未能挂钩 set_buffer，Axiom 的笔刷与工具改块将不计费，请检查 AxiomPaper 版本兼容性。");
            }
            return true;
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().log(Level.WARNING, "AxiomPaper 钩子安装失败", ex);
            return false;
        }
    }

    private static String describePaths(Map<String, String> paths) {
        if (paths.isEmpty()) {
            return "无";
        }
        StringBuilder builder = new StringBuilder();
        paths.forEach((channel, path) -> {
            if (!builder.isEmpty()) {
                builder.append("，");
            }
            builder.append(channel).append('=').append(path);
        });
        return builder.toString();
    }

    private AxiomLargePayloadHook.HandlerTable findHandlerTable(AxiomPaper axiomPaper) {
        try {
            AxiomLargePayloadHook.HandlerTable handlers = AxiomLargePayloadHook.findHandlers(axiomPaper);
            if (handlers == null) {
                plugin.getLogger().warning(
                        "未找到 Axiom 服务端处理器表（supportedServerboundPackets / 大载荷表），"
                                + "隧道或大载荷改块可能不计费。");
            }
            return handlers;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "读取 Axiom 服务端处理器表失败: " + ex.getMessage());
            return null;
        }
    }

    /**
     * 同时挂钩查表路径与 Bukkit 插件通道：
     * <ul>
     *   <li>Axiom 6：大包走 tunnel → supportedServerboundPackets，小包走 Bukkit</li>
     *   <li>Axiom 5：大包走 BigPayload 表，小包走 Bukkit</li>
     * </ul>
     */
    private void hookCharging(
            AxiomPaper axiomPaper,
            AxiomLargePayloadHook.HandlerTable handlerTable,
            Map<String, String> paths,
            String channel,
            boolean required,
            Function<AxiomPaper, PacketHandler> smallFactory,
            ChargingPacketHandlers.PacketGate gate
    ) throws ReflectiveOperationException {
        String channelName = "axiom:" + channel;
        boolean inTable = handlerTable != null && handlerTable.contains(channelName);
        Function<AxiomPaper, PacketHandler> wrappedFactory =
                axiom -> wrapCharging(smallFactory.apply(axiom), channel, gate);
        try {
            String path = replaceChannel(axiomPaper, handlerTable, channel, wrappedFactory,
                    inTable ? "隧道/查表+小载荷" : "小载荷");
            if (path != null) {
                paths.put(channel, path);
            } else if (required) {
                throw new IllegalStateException("required channel not hooked: " + channel);
            }
        } catch (RuntimeException | ReflectiveOperationException ex) {
            if (required) {
                throw ex;
            }
            plugin.getLogger().log(Level.WARNING,
                    "AxiomPaper 通道 " + channel + " 挂钩失败，该通道将不受管控: " + ex.getMessage());
        }
    }

    public static boolean isAxiomSessionActive(Player player) {
        if (player == null) {
            return false;
        }
        Plugin axiomPlugin = McwwsAxiomSurvivalPlugin.getInstance().getServer()
                .getPluginManager().getPlugin("AxiomPaper");
        if (!(axiomPlugin instanceof AxiomPaper axiomPaper)) {
            return false;
        }
        // Axiom 6 起 activeAxiomPlayers 为 private，改用公开 API
        return axiomPaper.canUseAxiom(player);
    }

    private PacketHandler wrapCharging(
            PacketHandler original,
            String channel,
            ChargingPacketHandlers.PacketGate gate
    ) {
        return ChargingPacketHandlers.wrap(plugin, chargeService, original, channel, gate);
    }

    /**
     * @return 挂钩路径描述；失败返回 null
     */
    private String replaceChannel(
            AxiomPaper axiomPaper,
            AxiomLargePayloadHook.HandlerTable handlerTable,
            String channel,
            Function<AxiomPaper, PacketHandler> wrappedFactory,
            String pathLabel
    ) throws ReflectiveOperationException {
        String channelName = "axiom:" + channel;
        PacketHandler wrapped = wrappedFactory.apply(axiomPaper);
        boolean updatedTable = false;
        if (handlerTable != null && handlerTable.contains(channelName)) {
            handlerTable.put(channelName, wrapped);
            updatedTable = true;
        }
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(axiomPaper, channelName);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(
                axiomPaper,
                channelName,
                new WrapperPacketListener(wrapped)
        );
        if (pathLabel != null) {
            return updatedTable ? pathLabel : "小载荷";
        }
        return updatedTable ? "隧道/查表+小载荷" : "小载荷";
    }
}
