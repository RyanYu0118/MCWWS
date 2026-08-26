package work.mcwws.ultimatetimberfix;

import com.songoda.ultimatetimber.events.TreeFallEvent;
import com.songoda.ultimatetimber.tree.DetectedTree;
import com.songoda.ultimatetimber.tree.ITreeBlock;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class UltimateTimberGuardListener implements Listener {

    private final McwwsUltimateTimberFixPlugin plugin;

    public UltimateTimberGuardListener(McwwsUltimateTimberFixPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTreeFall(TreeFallEvent event) {
        Player player = event.getPlayer();
        if (player != null && player.hasPermission(plugin.bypassPermission())) {
            return;
        }
        DetectedTree tree = event.getDetectedTree();
        if (!shouldProtect(tree)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!shouldProtect(block)) {
            return;
        }
        if (event.isCancelled()) {
            event.setCancelled(false);
        }
    }

    private boolean shouldProtect(DetectedTree tree) {
        if (SlimefunTreeDetector.isProtectedTree(tree)) {
            return true;
        }
        var detectedBlocks = tree.getDetectedTreeBlocks();
        if (detectedBlocks == null) {
            return false;
        }
        TreeFootprintStore store = plugin.getFootprintStore();
        if (store == null) {
            return false;
        }
        for (ITreeBlock<?> treeBlock : detectedBlocks.getAllTreeBlocks()) {
            Object raw = treeBlock.getBlock();
            if (raw instanceof Block block && store.contains(block.getLocation())) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldProtect(Block block) {
        if (SlimefunTreeDetector.isProtectedBlock(block)) {
            return true;
        }
        TreeFootprintStore store = plugin.getFootprintStore();
        return store != null && store.contains(block.getLocation());
    }
}
