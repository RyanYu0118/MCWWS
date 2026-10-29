package work.mcwws.axiomsurvival;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 插件自己切模式时打标，避免被当成管理员主动改模式写进恢复快照。 */
final class GameModeChanges {

    private static final Set<UUID> INTERNAL = ConcurrentHashMap.newKeySet();

    private GameModeChanges() {
    }

    static boolean isInternal(Player player) {
        return player != null && INTERNAL.contains(player.getUniqueId());
    }

    static void apply(Player player, GameMode mode) {
        if (player == null || mode == null || player.getGameMode() == mode) {
            return;
        }
        UUID id = player.getUniqueId();
        INTERNAL.add(id);
        try {
            player.setGameMode(mode);
        } finally {
            INTERNAL.remove(id);
        }
    }
}
