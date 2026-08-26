package work.mcwws.ultimatetimberfix;

import org.bukkit.Location;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class FellSession {

    private final UUID playerId;
    private final String saplingId;
    private final Location baseLocation;
    private final Set<String> blockKeys;
    private final Set<String> leafKeys;
    private long expiresAtMs;

    public FellSession(
            UUID playerId,
            String saplingId,
            Location baseLocation,
            Set<String> blockKeys,
            Set<String> leafKeys,
            long expiresAtMs
    ) {
        this.playerId = playerId;
        this.saplingId = saplingId;
        this.baseLocation = baseLocation.clone();
        this.blockKeys = Collections.unmodifiableSet(new LinkedHashSet<>(blockKeys));
        this.leafKeys = Collections.unmodifiableSet(new LinkedHashSet<>(leafKeys));
        this.expiresAtMs = expiresAtMs;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getSaplingId() {
        return saplingId;
    }

    public Location getBaseLocation() {
        return baseLocation.clone();
    }

    public Set<String> getBlockKeys() {
        return blockKeys;
    }

    public Set<String> getLeafKeys() {
        return leafKeys;
    }

    public boolean contains(Location location) {
        return blockKeys.contains(TreeFootprintStore.key(location));
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAtMs;
    }

    public void extend(long extraMs) {
        expiresAtMs = Math.max(expiresAtMs, System.currentTimeMillis() + extraMs);
    }
}
