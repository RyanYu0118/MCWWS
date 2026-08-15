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
                PacketType.Play.Server.SYSTEM_CHAT
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
                String plain = extractPlain(event.getPacket());
                if (plain == null || plain.isBlank()) {
                    return;
                }
                if (!host.throttle().allow(player.getUniqueId(), plain)) {
                    event.setCancelled(true);
                }
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

    private static String extractPlain(PacketContainer packet) {
        try {
            Object raw = packet.getModifier().withType(Component.class).readSafely(0);
            if (raw instanceof Component adventure) {
                return PlainTextComponentSerializer.plainText().serialize(adventure);
            }
        } catch (Throwable ignored) {
        }
        try {
            WrappedChatComponent wrapped = packet.getChatComponents().readSafely(0);
            if (wrapped != null) {
                String json = wrapped.getJson();
                if (json != null && !json.isBlank()) {
                    return roughPlainFromJson(json);
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            String legacy = packet.getStrings().readSafely(0);
            if (legacy != null && !legacy.isBlank()) {
                Component parsed = LegacyComponentSerializer.legacySection().deserialize(legacy);
                return PlainTextComponentSerializer.plainText().serialize(parsed);
            }
        } catch (Throwable ignored) {
        }
        return null;
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
}
