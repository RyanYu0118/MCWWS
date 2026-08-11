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
            replaceChannel(axiomPaper, "set_block", axiom -> wrapCharging(
                    new SetBlockPacketListener(axiom),
                    "set_block",
                    (player, buf) -> chargeService.evaluate(
                            player, "set_block", estimator.estimateSetBlockPacket(player, buf))
            ));
            replaceChannel(axiomPaper, "set_buffer", axiom -> wrapCharging(
                    new SetBlockBufferPacketListener(axiom),
                    "set_buffer",
                    (player, buf) -> {
                        PacketFeeEstimator.BufferEstimate estimate =
                                estimator.estimateSetBufferPacket(player, buf);
                        if (estimate.type() == 1) {
                            return chargeService.evaluateBiome(player, "set_buffer_biome", estimate.biomeCells());
                        }
                        return chargeService.evaluate(player, "set_buffer", estimate.blocks());
                    }
            ));
            // 以下通道在 Axiom 改版时更容易改名，单独兜底，避免连带影响方块扣费
            replaceChannelSafely(axiomPaper, "spawn_entity", axiom -> wrapCharging(
                    new SpawnEntityPacketListener(axiom),
                    "spawn_entity",
                    (player, buf) -> chargeService.evaluateEntities(
                            player, "spawn_entity", "生成实体", estimator.countCollection(buf))
            ));
            replaceChannelSafely(axiomPaper, "delete_entity", axiom -> wrapCharging(
                    new DeleteEntityPacketListener(axiom),
                    "delete_entity",
                    (player, buf) -> chargeService.evaluateEntities(
                            player, "delete_entity", "删除实体", estimator.countCollection(buf))
            ));
            replaceChannelSafely(axiomPaper, "manipulate_entity", axiom -> wrapCharging(
                    new ManipulateEntityPacketListener(axiom),
                    "manipulate_entity",
                    (player, buf) -> chargeService.evaluateEntities(
                            player, "manipulate_entity", "调整实体", estimator.countCollection(buf))
            ));
            replaceChannelSafely(axiomPaper, "set_world_time", axiom -> wrapCharging(
                    new SetTimePacketListener(axiom),
                    "set_world_time",
                    (player, buf) -> chargeService.evaluateWorldControl(
                            player, "world-control.block-world-time", "world-time-blocked")
            ));
            replaceChannelSafely(axiomPaper, "set_world_property", axiom -> wrapCharging(
                    new SetWorldPropertyListener(axiom),
                    "set_world_property",
                    (player, buf) -> chargeService.evaluateWorldControl(
                            player, "world-control.block-world-property", "world-property-blocked")
            ));
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
            plugin.getLogger().info(
                    "已挂钩 AxiomPaper 方块/实体/生物群系扣费、世界时间与属性拦截，以及 Editor 恢复（含生存 Editor 通道）。");
            return true;
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().log(Level.WARNING, "AxiomPaper 钩子安装失败", ex);
            return false;
        }
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

    private void replaceChannelSafely(
            AxiomPaper axiomPaper,
            String channel,
            Function<AxiomPaper, PacketHandler> wrappedFactory
    ) {
        try {
            replaceChannel(axiomPaper, channel, wrappedFactory);
        } catch (Throwable ex) {
            plugin.getLogger().log(Level.WARNING,
                    "AxiomPaper 通道 " + channel + " 挂钩失败，该通道将不受管控: " + ex.getMessage());
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
