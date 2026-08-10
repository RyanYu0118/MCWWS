package work.mcwws.axiomsurvival;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class EditorSessionState {

    private record Snapshot(GameMode gameMode, Location location, long capturedAtMs) {
    }

    private static final Map<UUID, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private EditorSessionState() {
    }

    static void capture(Player player) {
        if (player == null) {
            return;
        }
        GameMode mode = player.getGameMode();
        if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) {
            return;
        }
        Location location = player.getLocation();
        if (location.getWorld() == null) {
            return;
        }
        SNAPSHOTS.put(player.getUniqueId(), new Snapshot(mode, location.clone(), System.currentTimeMillis()));
    }

    static boolean has(Player player) {
        return player != null && SNAPSHOTS.containsKey(player.getUniqueId());
    }

    static void clear(Player player) {
        if (player != null) {
            SNAPSHOTS.remove(player.getUniqueId());
        }
    }

    static void restoreGamemode(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Snapshot snapshot = SNAPSHOTS.get(player.getUniqueId());
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
    }

    static void restoreLocation(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Snapshot snapshot = SNAPSHOTS.get(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        Location location = snapshot.location();
        if (location.getWorld() != null) {
            player.teleport(location);
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

    static boolean isNearSnapshot(Player player, double radius) {
        Snapshot snapshot = SNAPSHOTS.get(player.getUniqueId());
        if (snapshot == null || player.getWorld() != snapshot.location().getWorld()) {
            return false;
        }
        Location current = player.getLocation();
        Location saved = snapshot.location();
        if (current.distanceSquared(saved) <= radius * radius) {
            return true;
        }
        return Math.abs(current.getX() - saved.getX()) <= radius
                && Math.abs(current.getZ() - saved.getZ()) <= radius
                && Math.abs(current.getY() - saved.getY()) <= radius + 6D;
    }
}
