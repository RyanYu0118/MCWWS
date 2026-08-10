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
    private static final Map<UUID, Long> RESTORE_GRACE_UNTIL_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_AXIOM_ACTIVITY_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_EDITOR_TELEPORT_MS = new ConcurrentHashMap<>();

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
        long now = System.currentTimeMillis();
        SNAPSHOTS.put(player.getUniqueId(), new Snapshot(mode, location.clone(), now));
        touchActivity(player, now);
        McwwsAxiomSurvivalPlugin.getInstance().getLogger().info(
                "Editor 快照: " + player.getName() + " @ "
                        + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ()
        );
    }

    static boolean has(Player player) {
        return player != null && SNAPSHOTS.containsKey(player.getUniqueId());
    }

    static void clear(Player player) {
        if (player != null) {
            SNAPSHOTS.remove(player.getUniqueId());
            RESTORE_GRACE_UNTIL_MS.remove(player.getUniqueId());
            LAST_AXIOM_ACTIVITY_MS.remove(player.getUniqueId());
            LAST_EDITOR_TELEPORT_MS.remove(player.getUniqueId());
        }
    }

    static void touchActivity(Player player) {
        if (player != null) {
            touchActivity(player, System.currentTimeMillis());
        }
    }

    static void touchActivity(Player player, long timestampMs) {
        if (player != null) {
            LAST_AXIOM_ACTIVITY_MS.put(player.getUniqueId(), timestampMs);
        }
    }

    static long lastAxiomActivityMs(Player player) {
        if (player == null) {
            return 0L;
        }
        return LAST_AXIOM_ACTIVITY_MS.getOrDefault(player.getUniqueId(), 0L);
    }

    static void touchEditorTeleport(Player player) {
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        touchActivity(player, now);
        LAST_EDITOR_TELEPORT_MS.put(player.getUniqueId(), now);
    }

    static boolean shouldIdleRestore(Player player, long idleMs) {
        if (player == null || idleMs <= 0L) {
            return false;
        }
        Snapshot snapshot = SNAPSHOTS.get(player.getUniqueId());
        if (snapshot == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long lastTeleport = LAST_EDITOR_TELEPORT_MS.get(player.getUniqueId());
        if (lastTeleport != null && lastTeleport > snapshot.capturedAtMs()) {
            return now - lastTeleport >= idleMs;
        }
        long minimumMs = Math.max(idleMs, 3000L);
        return now - snapshot.capturedAtMs() >= minimumMs;
    }

    static void beginRestoreGrace(Player player, long millis) {
        if (player != null) {
            RESTORE_GRACE_UNTIL_MS.put(player.getUniqueId(), System.currentTimeMillis() + millis);
        }
    }

    static boolean isInRestoreGrace(Player player) {
        if (player == null) {
            return false;
        }
        Long until = RESTORE_GRACE_UNTIL_MS.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            RESTORE_GRACE_UNTIL_MS.remove(player.getUniqueId());
            return false;
        }
        return true;
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
        Location location = snapshot.location();
        if (location.getWorld() != null) {
            player.teleport(location);
        }
        if (player.getGameMode() != target) {
            player.setGameMode(target);
        }
        player.setFlying(false);
        player.setAllowFlight(false);
    }
}
