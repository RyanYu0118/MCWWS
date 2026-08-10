package work.mcwws.axiomsurvival;

import com.moulberry.axiom.AxiomPaper;
import com.moulberry.axiom.packet.PacketHandler;
import com.moulberry.axiom.packet.WrapperPacketListener;
import com.moulberry.axiom.packet.impl.SetBlockBufferPacketListener;
import com.moulberry.axiom.packet.impl.SetBlockPacketListener;
import com.moulberry.axiom.packet.impl.SetGamemodePacketListener;
import com.moulberry.axiom.packet.impl.SetNoPhysicalTriggerPacketListener;
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
            replaceChannel(axiomPaper, "set_block", axiom -> {
                PacketHandler original = new SetBlockPacketListener(axiom);
                return ChargingPacketHandlers.wrap(plugin, chargeService, estimator, original, "set_block");
            });
            replaceChannel(axiomPaper, "set_buffer", axiom -> {
                PacketHandler original = new SetBlockBufferPacketListener(axiom);
                return ChargingPacketHandlers.wrap(plugin, chargeService, estimator, original, "set_buffer");
            });
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
            plugin.getLogger().info("已挂钩 AxiomPaper set_block/set_buffer 扣费与 Editor 恢复（含生存 Editor 通道）。");
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
