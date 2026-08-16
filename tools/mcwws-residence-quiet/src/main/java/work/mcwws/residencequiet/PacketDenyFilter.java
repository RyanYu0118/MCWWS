package work.mcwws.residencequiet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

final class PacketDenyFilter {

    private final McwwsResidenceQuietPlugin plugin;
    private PacketAdapter adapter;

    PacketDenyFilter(McwwsResidenceQuietPlugin plugin) {
        this.plugin = plugin;
    }

    void register() {
        final McwwsResidenceQuietPlugin host = plugin;
        adapter = new PacketAdapter(
                host,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.SYSTEM_CHAT,
                PacketType.Play.Server.SET_ACTION_BAR_TEXT
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.isCancelled()) {
                    return;
                }
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }
                Extracted extracted = extract(event.getPacket());
                if (extracted.plain == null || extracted.plain.isBlank()) {
                    return;
                }
                String normalized = DenyThrottle.normalize(extracted.plain);
                boolean actionBar = isActionBar(event);
                boolean deny = host.throttle().isDenyTip(normalized);
                if (host.debug() && normalized.contains("权限")) {
                    host.getLogger().info("[deny-debug] type=" + event.getPacketType().name()
                            + " actionBar=" + actionBar + " deny=" + deny + " text=" + normalized);
                }
                if (!deny) {
                    return;
                }
                // 记下「这一 tick Residence 判了这名玩家没权限」，交互监听据此真正取消动作
                host.denySignal().mark(player.getUniqueId());
                if (!actionBar || !host.hud().useBossBar()) {
                    // 动作栏兜底模式：仍按「每趟停留只提醒一次」节流
                    if (!host.throttle().allow(player.getUniqueId(), normalized)) {
                        event.setCancelled(true);
                    }
                    return;
                }
                // Boss 栏模式：每次触发都重新计一轮，重复的由 DenyHud 按剩余倒计时挡掉
                event.setCancelled(true);
                Component title = extracted.visual != null
                        ? extracted.visual
                        : Component.text(normalized, NamedTextColor.RED);
                host.getServer().getScheduler().runTask(host, () ->
                        host.hud().show(player, normalized, title));
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(adapter);
    }

    void unregister() {
        if (adapter != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(adapter);
            adapter = null;
        }
    }

    private static boolean isActionBar(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SET_ACTION_BAR_TEXT) {
            return true;
        }
        try {
            Boolean overlay = event.getPacket().getBooleans().readSafely(0);
            if (overlay != null) {
                return overlay;
            }
        } catch (Throwable ignored) {
        }
        // Residence 把拒绝提示配成 ActionBar；读不到 overlay 时按动作栏处理
        return true;
    }

    /**
     * 逐个字段找文本：Paper 的 adventure 字段、NMS 聊天组件、退化的字符串都可能承载提示。
     */
    private static Extracted extract(PacketContainer packet) {
        StructureModifier<Object> modifier = packet.getModifier();
        int size = modifier.size();
        for (int i = 0; i < size; i++) {
            Object value = readSafely(modifier, i);
            if (value instanceof Component adventure) {
                Extracted hit = fromComponent(adventure);
                if (hit != null) {
                    return hit;
                }
            }
        }
        for (int i = 0; i < size; i++) {
            Object value = readSafely(modifier, i);
            if (value == null || !MinecraftReflection.isIChatBaseComponent(value.getClass())) {
                continue;
            }
            Extracted hit = fromNmsComponent(value);
            if (hit != null) {
                return hit;
            }
        }
        for (int i = 0; i < size; i++) {
            Object value = readSafely(modifier, i);
            if (!(value instanceof String legacy) || legacy.isBlank()) {
                continue;
            }
            Extracted hit = fromComponent(LegacyComponentSerializer.legacySection().deserialize(legacy));
            if (hit != null) {
                return hit;
            }
        }
        return new Extracted(null, null);
    }

    private static Object readSafely(StructureModifier<Object> modifier, int index) {
        try {
            return modifier.readSafely(index);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Extracted fromComponent(Component component) {
        if (component == null) {
            return null;
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(component);
        return plain.isBlank() ? null : new Extracted(plain, component);
    }

    private static Extracted fromNmsComponent(Object handle) {
        String json;
        try {
            WrappedChatComponent wrapped = WrappedChatComponent.fromHandle(handle);
            json = wrapped == null ? null : wrapped.getJson();
        } catch (Throwable ignored) {
            return null;
        }
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Extracted hit = fromComponent(GsonComponentSerializer.gson().deserialize(json));
            if (hit != null) {
                return hit;
            }
        } catch (Throwable ignored) {
        }
        String plain = roughPlainFromJson(json);
        return plain == null || plain.isBlank() ? null : new Extracted(plain, null);
    }

    private static String roughPlainFromJson(String json) {
        StringBuilder out = new StringBuilder();
        int idx = 0;
        while (idx < json.length()) {
            int key = json.indexOf("\"text\"", idx);
            if (key < 0) {
                break;
            }
            int colon = json.indexOf(':', key + 6);
            if (colon < 0) {
                break;
            }
            int startQuote = json.indexOf('"', colon + 1);
            if (startQuote < 0) {
                break;
            }
            int end = startQuote + 1;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == '\\' && end + 1 < json.length()) {
                    end += 2;
                    continue;
                }
                if (c == '"') {
                    break;
                }
                end++;
            }
            if (end > startQuote + 1) {
                out.append(json, startQuote + 1, end);
            }
            idx = end + 1;
        }
        return out.length() == 0 ? json : out.toString();
    }

    private record Extracted(String plain, Component visual) {
    }
}
