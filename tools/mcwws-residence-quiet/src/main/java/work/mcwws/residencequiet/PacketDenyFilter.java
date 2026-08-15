package work.mcwws.residencequiet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.kyori.adventure.text.Component;
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
                if (!host.throttle().isDenyTip(DenyThrottle.normalize(extracted.plain))) {
                    return;
                }
                boolean actionBar = isActionBar(event);
                if (!host.throttle().allow(player.getUniqueId(), extracted.plain)) {
                    event.setCancelled(true);
                    return;
                }
                if (!actionBar || !host.hud().useBossBar()) {
                    return;
                }
                event.setCancelled(true);
                Component title = extracted.visual != null
                        ? extracted.visual
                        : Component.text(DenyThrottle.normalize(extracted.plain));
                String fingerprint = DenyThrottle.normalize(extracted.plain);
                host.getServer().getScheduler().runTask(host, () ->
                        host.hud().show(player, fingerprint, title));
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

    private static Extracted extract(PacketContainer packet) {
        Component visual = null;
        String plain = null;
        try {
            Object raw = packet.getModifier().withType(Component.class).readSafely(0);
            if (raw instanceof Component adventure) {
                visual = adventure;
                plain = PlainTextComponentSerializer.plainText().serialize(adventure);
            }
        } catch (Throwable ignored) {
        }
        if (plain == null || plain.isBlank()) {
            try {
                WrappedChatComponent wrapped = packet.getChatComponents().readSafely(0);
                if (wrapped != null) {
                    String json = wrapped.getJson();
                    if (json != null && !json.isBlank()) {
                        plain = roughPlainFromJson(json);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (plain == null || plain.isBlank()) {
            try {
                String legacy = packet.getStrings().readSafely(0);
                if (legacy != null && !legacy.isBlank()) {
                    visual = LegacyComponentSerializer.legacySection().deserialize(legacy);
                    plain = PlainTextComponentSerializer.plainText().serialize(visual);
                }
            } catch (Throwable ignored) {
            }
        }
        return new Extracted(plain, visual);
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
