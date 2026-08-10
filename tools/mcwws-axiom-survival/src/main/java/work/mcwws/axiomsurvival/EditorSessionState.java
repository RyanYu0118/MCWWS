package work.mcwws.axiomsurvival;

import org.bukkit.Bukkit;
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
    private static final Map<UUID, Long> RESTORE_GRACE_UNTIL_MS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ENTERED_AT_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_AXIOM_TELEPORT_TICK = new ConcurrentHashMap<>();

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
        UUID id = player.getUniqueId();
        SNAPSHOTS.put(id, new Snapshot(mode, location.clone()));
        ENTERED_AT_TICK.put(id, Bukkit.getCurrentTick());
        LAST_AXIOM_TELEPORT_TICK.remove(id);
        McwwsAxiomSurvivalPlugin.getInstance().getLogger().info(
                "Editor 快照: " + player.getName() + " @ "
                        + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ()
        );
    }

    static boolean has(Player player) {
        return player != null && SNAPSHOTS.containsKey(player.getUniqueId());
    }

    static void clear(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUniqueId();
        SNAPSHOTS.remove(id);
        RESTORE_GRACE_UNTIL_MS.remove(id);
        ENTERED_AT_TICK.remove(id);
        LAST_AXIOM_TELEPORT_TICK.remove(id);
    }

    static void touchAxiomTeleport(Player player) {
        if (player != null) {
            LAST_AXIOM_TELEPORT_TICK.put(player.getUniqueId(), Bukkit.getCurrentTick());
        }
    }

    static boolean shouldRestoreOnVanillaMove(Player player, int enterGraceTicks) {
        if (player == null || !has(player) || player.getGameMode() != GameMode.SPECTATOR) {
            return false;
        }
        UUID id = player.getUniqueId();
        int now = Bukkit.getCurrentTick();
        Integer enteredAt = ENTERED_AT_TICK.get(id);
        if (enteredAt != null && now - enteredAt < Math.max(enterGraceTicks, 0)) {
            return false;
        }
        Integer lastAxiom = LAST_AXIOM_TELEPORT_TICK.get(id);
        if (lastAxiom != null && now - lastAxiom < VANILLA_MOVE_BUFFER_TICKS) {
            return false;
        }
        return lastAxiom != null || (enteredAt != null && now - enteredAt >= Math.max(enterGraceTicks, 0));
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
        ENTERED_AT_TICK.remove(player.getUniqueId());
        LAST_AXIOM_TELEPORT_TICK.remove(player.getUniqueId());
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
