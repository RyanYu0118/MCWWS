package work.mcwws.worldedit;

import com.sk89q.worldedit.regions.Region;
import org.bukkit.entity.Player;

final class EstimateContext {

    private static final ThreadLocal<Player> PLAYER = new ThreadLocal<>();
    private static final ThreadLocal<Region> REGION = new ThreadLocal<>();

    private EstimateContext() {
    }

    static void setPlayer(Player player) {
        if (player == null) {
            PLAYER.remove();
        } else {
            PLAYER.set(player);
        }
    }

    static void setRegion(Region region) {
        if (region == null) {
            REGION.remove();
        } else {
            REGION.set(region);
        }
    }

    static Player player() {
        return PLAYER.get();
    }

    static Region region() {
        return REGION.get();
    }

    static void clear() {
        PLAYER.remove();
        REGION.remove();
    }
}
