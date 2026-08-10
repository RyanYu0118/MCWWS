package work.mcwws.axiomsurvival;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class EditorSessionState {

    private record Snapshot(GameMode gameMode, Location location) {
    }

    private static final Map<UUID, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private EditorSessionState() {
    }

    static void capture(Player player) {
        if (player == null || !BlockProtection.isSurvivalLike(player)) {
            return;
        }
        Location location = player.getLocation();
        if (location.getWorld() == null) {
            return;
        }
        SNAPSHOTS.put(player.getUniqueId(), new Snapshot(player.getGameMode(), location.clone()));
    }

    static boolean has(Player player) {
        return player != null && SNAPSHOTS.containsKey(player.getUniqueId());
    }

    static void clear(Player player) {
        if (player != null) {
            SNAPSHOTS.remove(player.getUniqueId());
        }
    }

    static void restoreAndClear(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Snapshot snapshot = SNAPSHOTS.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        GameMode target = snapshot.gameMode();
        if (target != GameMode.SURVIVAL && target != GameMode.ADVENTURE) {
            target = GameMode.SURVIVAL;
        }
        if (player.getGameMode() != target) {
            player.setGameMode(target);
        }
        Location location = snapshot.location();
        if (location.getWorld() != null) {
            player.teleport(location);
        }
    }
}
