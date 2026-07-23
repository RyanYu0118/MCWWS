package work.mcwws.economyledger;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * 业务插件在发起 Vault/Essentials 扣款前设置上下文，经济层会优先使用更准确的分类与说明。
 * <pre>
 * LedgerContext.runWith(player, "worldedit", "创世神建造", "we-123", () -&gt; economy.withdraw(...));
 * </pre>
 */
public final class LedgerContext {

    public record Entry(UUID uuid, String category, String description, String refId) {
    }

    private static final ThreadLocal<Entry> CURRENT = new ThreadLocal<>();

    private LedgerContext() {
    }

    public static void set(Player player, String category, String description, String refId) {
        if (player == null) {
            return;
        }
        CURRENT.set(new Entry(
                player.getUniqueId(),
                sanitize(category),
                sanitize(description),
                sanitize(refId)
        ));
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static void runWith(Player player, String category, String description, String refId, Runnable action) {
        set(player, category, description, refId);
        try {
            action.run();
        } finally {
            clear();
        }
    }

    static Optional<Entry> peek(UUID uuid) {
        Entry entry = CURRENT.get();
        if (entry == null || uuid == null || !uuid.equals(entry.uuid())) {
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }
}
