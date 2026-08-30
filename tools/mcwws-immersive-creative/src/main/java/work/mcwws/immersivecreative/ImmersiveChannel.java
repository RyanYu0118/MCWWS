package work.mcwws.immersivecreative;

import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class ImmersiveChannel implements PluginMessageListener {

    public static final String CHANNEL = "mcwws:immersive_creative";
    /** 1.0.3 及更早的客户端只发材质 + 数量，服务端据此重建物品会抹掉附魔与 Slimefun 数据，必须拒收。 */
    private static final byte OP_SLOT_LEGACY = 1;
    private static final byte OP_SLOT_NBT = 2;

    private final McwwsImmersiveCreativePlugin plugin;

    public ImmersiveChannel(McwwsImmersiveCreativePlugin plugin) {
        this.plugin = plugin;
    }

    public void sendState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        boolean enabled = plugin.state().isEnabled(player);
        byte[] payload = new byte[] { enabled ? (byte) 1 : (byte) 0 };
        player.sendPluginMessage(plugin, CHANNEL, payload);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel) || player == null || message == null || message.length < 1) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            byte op = in.readByte();
            if (op == OP_SLOT_LEGACY) {
                rejectLegacyClient(player);
                return;
            }
            if (op != OP_SLOT_NBT) {
                return;
            }
            int slot = in.readInt();
            byte[] raw = new byte[in.readInt()];
            in.readFully(raw);
            String snbt = new String(raw, StandardCharsets.UTF_8);
            ItemStack stack = toStack(snbt);
            if (plugin.debug()) {
                plugin.getLogger().info("[debug] 通道槽位: " + player.getName()
                        + " slot=" + slot + " item=" + stack.getType() + " x" + stack.getAmount());
            }
            plugin.creativeSlots().applyFromClient(player, slot, stack);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "解析创造槽位消息失败", ex);
        }
    }

    /** 旧客户端会导致物品丢数据，直接给它关掉沉浸式创造，别等玩家发现附魔没了。 */
    private void rejectLegacyClient(Player player) {
        if (!plugin.state().isEnabled(player)) {
            return;
        }
        plugin.state().setEnabled(player, false);
        sendState(player);
        plugin.send(player, "messages.client-outdated");
        plugin.getLogger().warning("玩家 " + player.getName()
                + " 的客户端模组过旧（仅上报材质），已强制关闭沉浸式创造。");
    }

    private static ItemStack toStack(String snbt) {
        if (snbt == null || snbt.isBlank()) {
            return new ItemStack(Material.AIR);
        }
        ItemStack stack = NBT.itemStackFromNBT(NBT.parseNBT(snbt));
        return stack == null ? new ItemStack(Material.AIR) : stack;
    }

    public static boolean clientPresent(Player player) {
        if (player == null) {
            return false;
        }
        return player.getListeningPluginChannels().contains(CHANNEL);
    }
}
