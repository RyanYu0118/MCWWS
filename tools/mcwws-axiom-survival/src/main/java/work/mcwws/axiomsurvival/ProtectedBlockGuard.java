package work.mcwws.axiomsurvival;

import org.bukkit.block.BlockState;

import java.util.List;

/**
 * Slimefun 机器与不可破坏方块的写入回填。
 *
 * <p>Axiom 的改块在 {@code addPendingOperation} 里异步落地，无法像创世神那样在写入前逐格拦下，
 * 因此这里改为「记录原状 → 让 Axiom 写完 → 按快照还原」。{@code BlockState} 快照包含容器内容，
 * 还原时不触发物理更新，避免连带破坏周围结构。
 */
final class ProtectedBlockGuard {

    private ProtectedBlockGuard() {
    }

    static boolean enabled() {
        McwwsAxiomSurvivalPlugin plugin = McwwsAxiomSurvivalPlugin.getInstance();
        return plugin != null
                && plugin.getPluginConfig().getBoolean("protection.restore-protected-blocks", true);
    }

    static void scheduleRestore(List<BlockState> states) {
        McwwsAxiomSurvivalPlugin plugin = McwwsAxiomSurvivalPlugin.getInstance();
        if (plugin == null || states == null || states.isEmpty() || !enabled()) {
            return;
        }
        long delay = Math.max(plugin.getPluginConfig().getLong("protection.restore-delay-ticks", 5L), 1L);
        List<BlockState> snapshot = List.copyOf(states);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> restore(plugin, snapshot), delay);
        // Axiom 的待办操作可能跨多 tick 落地，稍后再补一次
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> restore(plugin, snapshot), delay * 8L);
    }

    private static void restore(McwwsAxiomSurvivalPlugin plugin, List<BlockState> states) {
        for (BlockState state : states) {
            try {
                state.update(true, false);
            } catch (Throwable ex) {
                plugin.getLogger().fine("受保护方块还原失败: " + ex.getMessage());
            }
        }
    }
}
