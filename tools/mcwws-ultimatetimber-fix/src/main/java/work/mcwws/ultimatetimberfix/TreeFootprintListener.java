package work.mcwws.ultimatetimberfix;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.StructureGrowEvent;

public final class TreeFootprintListener implements Listener {

    private final McwwsUltimateTimberFixPlugin plugin;

    public TreeFootprintListener(McwwsUltimateTimberFixPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        Location location = event.getLocation();
        String id = SlimefunTreeDetector.slimefunIdAt(location.getBlock());
        if (!SlimefunTreeDetector.isExoticGardenSaplingId(id)) {
            return;
        }
        scheduleFootprintScan(location);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSaplingPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        String id = SlimefunTreeDetector.slimefunIdAt(block);
        if (!SlimefunTreeDetector.isExoticGardenSaplingId(id)) {
            return;
        }
        plugin.getFootprintStore().registerFromOrigin(block.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreeBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        TreeFootprintStore store = plugin.getFootprintStore();
        if (!store.contains(block.getLocation())) {
            return;
        }
        store.removeFootprint(block.getLocation());
    }

    private void scheduleFootprintScan(Location origin) {
        long delay = plugin.footprintScanDelayTicks();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getFootprintStore().registerFromOrigin(origin);
        }, delay);
    }
}
