package work.mcwws.ultimatetimberfix;

import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FellSessionManager {

    private final Map<UUID, FellSession> sessionsByPlayer = new ConcurrentHashMap<>();

    public void start(FellSession session) {
        sessionsByPlayer.put(session.getPlayerId(), session);
    }

    public FellSession get(UUID playerId) {
        FellSession session = sessionsByPlayer.get(playerId);
        if (session == null || session.isExpired()) {
            if (session != null) {
                sessionsByPlayer.remove(playerId);
            }
            return null;
        }
        return session;
    }

    public FellSession findAt(Location location) {
        if (location == null) {
            return null;
        }
        String key = TreeFootprintStore.key(location);
        for (FellSession session : sessionsByPlayer.values()) {
            if (session.isExpired()) {
                continue;
            }
            if (session.getBlockKeys().contains(key)) {
                return session;
            }
        }
        return null;
    }

    public FellSession end(UUID playerId) {
        return sessionsByPlayer.remove(playerId);
    }

    public void endDelayed(UUID playerId, long delayTicks, McwwsUltimateTimberFixPlugin plugin) {
        FellSession session = sessionsByPlayer.get(playerId);
        if (session == null) {
            return;
        }
        session.extend(delayTicks * 50L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> sessionsByPlayer.remove(playerId), delayTicks);
    }

    public void cleanupExpired() {
        sessionsByPlayer.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
