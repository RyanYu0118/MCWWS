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
            Map<String, PacketHandler> largeHandlers = findLargePayloadHandlers();
            Map<String, String> paths = new LinkedHashMap<>();

            hookCharging(axiomPaper, largeHandlers, paths, "set_block", true,
                    SetBlockPacketListener::new,
                    (player, buf) -> chargeService.evaluate(
                            player, "set_block", estimator.estimateSetBlockPacket(player, buf))
            );
            hookCharging(axiomPaper, largeHandlers, paths, "set_buffer", true,
                    SetBlockBufferPacketListener::new,
                    (player, buf) -> {
                        PacketFeeEstimator.BufferEstimate estimate =
                                estimator.estimateSetBufferPacket(player, buf);
                        if (estimate.type() == 1) {
                            return chargeService.evaluateBiome(player, "set_buffer_biome", estimate.biomeCells());
                        }
                        return chargeService.evaluate(player, "set_buffer", estimate.blocks());
                    }
            );
            // 以下通道在 Axiom 改版时更容易改名，单独兜底，避免连带影响方块扣费
            hookCharging(axiomPaper, largeHandlers, paths, "spawn_entity", false,
                    SpawnEntityPacketListener::new,
                    (player, buf) -> chargeService.evaluateEntities(
                            player, "spawn_entity", "生成实体", estimator.countCollection(buf))
            );
            hookCharging(axiomPaper, largeHandlers, paths, "delete_entity", false,
                    DeleteEntityPacketListener::new,
                    (player, buf) -> chargeService.evaluateEntities(
                            player, "delete_entity", "删除实体", estimator.countCollection(buf))
            );
            hookCharging(axiomPaper, largeHandlers, paths, "manipulate_entity", false,
                    ManipulateEntityPacketListener::new,
                    (player, buf) -> chargeService.evaluateEntities(
                            player, "manipulate_entity", "调整实体", estimator.countCollection(buf))
            );
            hookCharging(axiomPaper, largeHandlers, paths, "set_world_time", false,
                    SetTimePacketListener::new,
                    (player, buf) -> chargeService.evaluateWorldControl(
                            player, "world-control.block-world-time", "world-time-blocked")
            );
            hookCharging(axiomPaper, largeHandlers, paths, "set_world_property", false,
                    SetWorldPropertyListener::new,
                    (player, buf) -> chargeService.evaluateWorldControl(
                            player, "world-control.block-world-property", "world-property-blocked")
            );
            replaceChannel(axiomPaper, "set_gamemode", axiom -> {
                PacketHandler original = new SetGamemodePacketListener(axiom);
                return EditorPacketHandlers.wrapGamemode(plugin, editorRestoreService, survivalEditorService, original);
            });
            replaceChannel(axiomPaper, "teleport", axiom -> {
                PacketHandler original = new TeleportPacketListener(axiom);
                return EditorPacketHandlers.wrapTeleport(plugin, editorRestoreService, survivalEditorService, original);
            });
            replaceChannel(axiomPaper, "set_no_physical_trigger", axiom -> {
                PacketHandler original = new SetNoPhysicalTriggerPacketListener(axiom);
                return EditorPacketHandlers.wrapNoPhysicalTrigger(plugin, editorRestoreService, survivalEditorService, original);
            });
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

    private Map<String, PacketHandler> findLargePayloadHandlers() {
        try {
            Map<String, PacketHandler> handlers = AxiomLargePayloadHook.findHandlers();
            if (handlers == null) {
                plugin.getLogger().warning("未找到 Axiom 大载荷处理器表，大载荷改块可能不计费。");
            }
            return handlers;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "读取 Axiom 大载荷处理器表失败: " + ex.getMessage());
            return null;
        }
    }

    /**
     * 按 Axiom 自己的分流方式挂钩：通道在大载荷表里就换表内处理器（Bukkit 侧本就是 Dummy，
     * 不去动它以免改变 Axiom 行为），否则换 Bukkit 通道监听器。
     */
    private void hookCharging(
            AxiomPaper axiomPaper,
            Map<String, PacketHandler> largeHandlers,
            Map<String, String> paths,
            String channel,
            boolean required,
            Function<AxiomPaper, PacketHandler> smallFactory,
            ChargingPacketHandlers.PacketGate gate
    ) throws ReflectiveOperationException {
        String channelName = "axiom:" + channel;
        PacketHandler large = largeHandlers == null ? null : largeHandlers.get(channelName);
        if (large != null) {
            try {
                largeHandlers.put(channelName, wrapCharging(large, channel, gate));
                paths.put(channel, "大载荷");
                return;
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.WARNING,
                        "写入 Axiom 大载荷处理器表失败 (" + channel + "): " + ex.getMessage());
                if (required) {
                    throw ex;
                }
                return;
            }
        }
        Function<AxiomPaper, PacketHandler> wrappedFactory =
                axiom -> wrapCharging(smallFactory.apply(axiom), channel, gate);
        if (required) {
            replaceChannel(axiomPaper, channel, wrappedFactory);
        } else if (!replaceChannelSafely(axiomPaper, channel, wrappedFactory)) {
            return;
        }
        paths.put(channel, "小载荷");
    }

    public static boolean isAxiomSessionActive(Player player) {
        if (player == null) {
            return false;
        }
        Plugin axiomPlugin = McwwsAxiomSurvivalPlugin.getInstance().getServer().getPluginManager().getPlugin("AxiomPaper");
        if (!(axiomPlugin instanceof AxiomPaper axiomPaper)) {
            return false;
        }
        return axiomPaper.activeAxiomPlayers.contains(player.getUniqueId());
    }

    private PacketHandler wrapCharging(
            PacketHandler original,
            String channel,
            ChargingPacketHandlers.PacketGate gate
    ) {
        return ChargingPacketHandlers.wrap(plugin, chargeService, original, channel, gate);
    }

    private boolean replaceChannelSafely(
            AxiomPaper axiomPaper,
            String channel,
            Function<AxiomPaper, PacketHandler> wrappedFactory
    ) {
        try {
            replaceChannel(axiomPaper, channel, wrappedFactory);
            return true;
        } catch (Throwable ex) {
            plugin.getLogger().log(Level.WARNING,
                    "AxiomPaper 通道 " + channel + " 挂钩失败，该通道将不受管控: " + ex.getMessage());
            return false;
        }
    }

    private void replaceChannel(AxiomPaper axiomPaper, String channel, Function<AxiomPaper, PacketHandler> wrappedFactory)
            throws ReflectiveOperationException {
        String channelName = "axiom:" + channel;
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(axiomPaper, channelName);
        PacketHandler wrapped = wrappedFactory.apply(axiomPaper);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(
                axiomPaper,
                channelName,
                new WrapperPacketListener(wrapped)
        );
    }
}
