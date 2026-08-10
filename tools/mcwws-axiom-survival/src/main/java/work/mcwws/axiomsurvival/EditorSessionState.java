package work.mcwws.axiomsurvival;

import org.bukkit.Bukkit;
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
    private static final Map<UUID, Integer> LAST_AXIOM_TELEPORT_TICK = new ConcurrentHashMap<>();

    /** Axiom 相机 teleport 与 vanilla 移动包之间的最小间隔（tick） */
    private static final int VANILLA_MOVE_BUFFER_TICKS = 2;

    private EditorSessionState() {
    }

    static void capture(Player player) {
        if (player == null || has(player)) {
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
            LAST_AXIOM_TELEPORT_TICK.remove(player.getUniqueId());
        }
    }

    static void touchAxiomTeleport(Player player) {
        if (player != null) {
            LAST_AXIOM_TELEPORT_TICK.put(player.getUniqueId(), Bukkit.getCurrentTick());
        }
    }

    static boolean shouldRestoreOnVanillaMove(Player player) {
        if (player == null || !has(player)) {
            return false;
        }
        Integer lastTeleportTick = LAST_AXIOM_TELEPORT_TICK.get(player.getUniqueId());
        if (lastTeleportTick == null) {
            return true;
        }
        return Bukkit.getCurrentTick() - lastTeleportTick >= VANILLA_MOVE_BUFFER_TICKS;
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
