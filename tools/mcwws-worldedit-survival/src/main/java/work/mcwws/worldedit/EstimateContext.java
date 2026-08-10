package work.mcwws.worldedit;

import org.bukkit.entity.Player;

final class EstimateContext {

    private static final ThreadLocal<Player> PLAYER = new ThreadLocal<>();

    private EstimateContext() {
    }

    static void setPlayer(Player player) {
        if (player == null) {
            PLAYER.remove();
        } else {
            PLAYER.set(player);
        }
    }

    static Player player() {
        return PLAYER.get();
    }

    static void clear() {
        PLAYER.remove();
    }
}
