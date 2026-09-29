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
    /** 客户端进服后主动要一次开关，避免 Join 时通道未就绪导致状态包被丢弃。 */
    private static final byte OP_REQUEST_STATE = 10;
    /** 1.0.4 及更早的客户端不带光标内容，服务端只能靠额度池猜测，会漏扣费，必须拒收。 */
    private static final byte OP_SLOT_LEGACY = 1;
    private static final byte OP_SLOT_NBT_NO_CURSOR = 2;
    /** 槽位新内容 + 操作后的光标内容，服务端据此按「背包 + 光标」整体算净差额。 */
    private static final byte OP_SLOT_WITH_CURSOR = 3;

    private final McwwsImmersiveCreativePlugin plugin;

    public ImmersiveChannel(McwwsImmersiveCreativePlugin plugin) {
        this.plugin = plugin;
    }

    public void sendState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        // 通道未注册时 Bukkit 会静默丢包；调用方应重试 / 等 RegisterChannel
        if (!clientPresent(player)) {
            return;
        }
        boolean enabled = plugin.state().isEnabled(player);
        byte[] payload = new byte[] { enabled ? (byte) 1 : (byte) 0 };
        player.sendPluginMessage(plugin, CHANNEL, payload);
        if (plugin.debug()) {
            plugin.getLogger().info("[debug] 同步开关 -> " + player.getName() + " enabled=" + enabled);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel) || player == null || message == null || message.length < 1) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            byte op = in.readByte();
            if (op == OP_REQUEST_STATE) {
                sendState(player);
                return;
            }
            if (op == OP_SLOT_LEGACY || op == OP_SLOT_NBT_NO_CURSOR) {
                rejectLegacyClient(player);
                return;
            }
            if (op != OP_SLOT_WITH_CURSOR) {
                return;
            }
            int slot = in.readInt();
            String itemSnbt = readString(in);
            String carriedSnbt = readString(in);
            ItemStack stack;
            ItemStack carried;
            try {
                stack = toStack(itemSnbt);
                carried = toStack(carriedSnbt);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "解析创造槽位消息失败: " + previewSnbt(itemSnbt), ex);
                rejectUnparseable(player);
                return;
            }
            if (unparseable(itemSnbt, stack) || unparseable(carriedSnbt, carried)) {
                plugin.getLogger().warning("无法还原创造栏物品，已拒绝: " + previewSnbt(itemSnbt));
                rejectUnparseable(player);
                return;
            }
            if (plugin.debug()) {
                plugin.getLogger().info("[debug] 通道槽位: " + player.getName()
                        + " slot=" + slot + " item=" + stack.getType() + " x" + stack.getAmount()
                        + " 光标=" + carried.getType() + " x" + carried.getAmount());
            }
            plugin.creativeSlots().applyFromClient(player, slot, stack, carried);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "解析创造槽位消息失败", ex);
            rejectUnparseable(player);
        }
    }

    /**
     * SNBT 对不上本服物品注册表时（例如 26.3 客户端创造栏里的羊毛楼梯），
     * 绝不能静默丢包，否则客户端预测物品会留在身上且不扣费。
     */
    private void rejectUnparseable(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (plugin.creativeSlots() != null) {
            plugin.creativeSlots().forget(player.getUniqueId());
        }
        player.setItemOnCursor(new ItemStack(Material.AIR));
        player.closeInventory();
        player.updateInventory();
        if (plugin.state().isEnabled(player)) {
            plugin.send(player, "messages.unknown-item");
        }
    }

    private static boolean unparseable(String snbt, ItemStack stack) {
        if (snbt == null || snbt.isBlank()) {
            return false;
        }
        return stack == null || stack.getType().isAir();
    }

    private static String previewSnbt(String snbt) {
        if (snbt == null || snbt.isBlank()) {
            return "(empty)";
        }
        return snbt.length() <= 180 ? snbt : snbt.substring(0, 180) + "...";
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
                + " 的客户端模组过旧（未上报光标），已强制关闭沉浸式创造。");
    }

    private static String readString(DataInputStream in) throws java.io.IOException {
        byte[] raw = new byte[in.readInt()];
        in.readFully(raw);
        return new String(raw, StandardCharsets.UTF_8);
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
