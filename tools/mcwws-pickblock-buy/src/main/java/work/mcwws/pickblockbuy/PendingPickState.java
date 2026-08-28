package work.mcwws.pickblockbuy;

import org.bukkit.Material;

public record PendingPickState(Material material, long timestampMillis) {
}
