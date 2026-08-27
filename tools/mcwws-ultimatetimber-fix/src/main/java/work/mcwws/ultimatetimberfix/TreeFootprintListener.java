package work.mcwws.ultimatetimberfix;

import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.StructureGrowEvent;

/**
 * 登记 ExoticGarden 果树占地，并在生长/砍伐前清掉 UltimateTimber 的「放置方块」标记。
 * <p>
 * ExoticGarden 会 cancel {@link StructureGrowEvent}，UT 自带的 MONITOR+ignoreCancelled
 * 清标记逻辑不会执行，导致树基永远被当成玩家放置方块、连根砍失败。
 */
public final class TreeFootprintListener implements Listener {

    private final McwwsUltimateTimberFixPlugin plugin;

    public TreeFootprintListener(McwwsUltimateTimberFixPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 必须 ignoreCancelled=false：EG 已取消事件，但仍需登记 footprint 并清 UT 放置标记。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onStructureGrow(StructureGrowEvent event) {
        Location location = event.getLocation();
        TreeFootprintStore store = plugin.getFootprintStore();

        // EG 可能已拆掉树苗方块，优先用放置时登记的 saplingId
        String saplingId = store.getSaplingId(location);
        if (saplingId == null) {
            saplingId = SlimefunTreeDetector.resolveSaplingId(location.getBlock());
        }
        if (!SlimefunTreeDetector.isFruitTreeSaplingId(saplingId)) {
            return;
        }

        // 立刻清树苗格标记（不必等 schematic 粘贴完）
        int cleared = UltimateTimberPlacedBlocks.clear(location);
        if (cleared > 0) {
            plugin.getLogger().info("已清除 ExoticGarden 树苗的 UT 放置标记 @" + format(location));
        }

        scheduleFootprintScan(location, saplingId);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSaplingPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        String saplingId = SlimefunTreeDetector.resolveSaplingId(block);
        if (!SlimefunTreeDetector.isFruitTreeSaplingId(saplingId)) {
            return;
        }
        plugin.getFootprintStore().registerFromOrigin(block.getLocation(), saplingId);
    }

    /**
     * 在 UltimateTimber（HIGHEST）检测之前清掉果树的放置标记，修复已长成但仍被标记的树。
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTreeBreakClearPlaced(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!Tag.LOGS.isTagged(block.getType()) && !SlimefunTreeDetector.isTreePart(block.getType())) {
            return;
        }

        TreeFootprintStore store = plugin.getFootprintStore();
        boolean knownFruitTree = store.contains(block.getLocation())
                || store.getSaplingId(block.getLocation()) != null
                || SlimefunTreeDetector.isFruitTreeBlock(block)
                || hasNearbyFruitMarker(block);

        if (!knownFruitTree && !UltimateTimberPlacedBlocks.isMarkedPlaced(block)) {
            return;
        }
        if (!knownFruitTree) {
            // 仅当放置标记存在且附近有果树特征时才清，避免误伤普通树
            if (!hasNearbyFruitMarker(block)) {
                return;
            }
        }

        int cleared = UltimateTimberPlacedBlocks.clearTreeAround(block, plugin.footprintMaxBlocks());
        if (cleared > 0) {
            plugin.getLogger().info("砍伐前清除 UT 放置标记 " + cleared + " 格 @" + format(block.getLocation()));
        }
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

    private void scheduleFootprintScan(Location origin, String saplingId) {
        long delay = plugin.footprintScanDelayTicks();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getFootprintStore().registerFromOrigin(origin, saplingId);
            int cleared = UltimateTimberPlacedBlocks.clearTreeAround(origin.getBlock(), plugin.footprintMaxBlocks());
            if (cleared > 0) {
                plugin.getLogger().info("生长后清除 UT 放置标记 " + cleared + " 格 @" + format(origin));
            }
        }, delay);
    }

    /**
     * 在半径内找 ExoticGarden 果实头颅 / 已登记 footprint，用于识别「已长成果树」。
     */
    private boolean hasNearbyFruitMarker(Block origin) {
        TreeFootprintStore store = plugin.getFootprintStore();
        int radius = 6;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = 0; dy <= 12; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    Block relative = origin.getRelative(dx, dy, dz);
                    if (store.contains(relative.getLocation())) {
                        return true;
                    }
                    if (SlimefunTreeDetector.isFruitTreeBlock(relative)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String format(Location location) {
        return location.getWorld().getName()
                + ' ' + location.getBlockX()
                + ' ' + location.getBlockY()
                + ' ' + location.getBlockZ();
    }
}
