package work.mcwws.axiomsurvival;

import org.bukkit.entity.Player;

/**
 * Axiom 编辑器用 NMS {@code abilities.flyingSpeed}（原版默认 0.05）。
 * Bukkit {@link Player#setFlySpeed(float)} 的合法范围是 -1..1，对应 NMS 的 -0.5..0.5。
 */
final class FlySpeeds {

    private static final float DEFAULT_NMS = 0.05f;

    private FlySpeeds() {
    }

    static void applyNms(Player player, Float nmsSpeed) {
        if (player == null || !player.isOnline() || nmsSpeed == null) {
            return;
        }
        if (!Float.isFinite(nmsSpeed) || nmsSpeed <= 0f) {
            return;
        }
        float clampedNms = Math.max(DEFAULT_NMS, Math.min(1f, nmsSpeed));
        float bukkit = Math.max(-1f, Math.min(1f, clampedNms * 2f));
        try {
            player.setFlySpeed(bukkit);
        } catch (IllegalArgumentException ex) {
            McwwsAxiomSurvivalPlugin.getInstance().getLogger().fine(
                    "无法设置飞行速度 " + bukkit + ": " + ex.getMessage()
            );
        }
    }
}
