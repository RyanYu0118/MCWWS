package work.mcwws.worldsync;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.InventoryHolder;

public final class DirtyListener implements Listener {
    private final McwwsWorldSyncPlugin plugin;

    public DirtyListener(McwwsWorldSyncPlugin plugin) {
        this.plugin = plugin;
    }

    private void mark(Block block) {
        if (!plugin.lock().hasLock() || block == null) {
            return;
        }
        plugin.dirty().markChunk(block.getWorld(), block.getChunk(), plugin.serverRoot());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlace(BlockPlaceEvent e) {
        mark(e.getBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent e) {
        mark(e.getBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFromTo(BlockFromToEvent e) {
        mark(e.getBlock());
        mark(e.getToBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBurn(BlockBurnEvent e) {
        mark(e.getBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFade(BlockFadeEvent e) {
        mark(e.getBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onGrow(BlockGrowEvent e) {
        mark(e.getBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onForm(BlockFormEvent e) {
        mark(e.getBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChange(EntityChangeBlockEvent e) {
        mark(e.getBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().forEach(this::mark);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent e) {
        mark(e.getBlock());
        e.blockList().forEach(this::mark);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        mark(e.getBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBucketFill(PlayerBucketFillEvent e) {
        mark(e.getBlock());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTree(StructureGrowEvent e) {
        e.getBlocks().forEach(s -> mark(s.getBlock()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInv(InventoryCloseEvent e) {
        if (!plugin.lock().hasLock()) {
            return;
        }
        InventoryHolder holder = e.getInventory().getHolder();
        if (holder instanceof org.bukkit.block.BlockState state) {
            mark(state.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnload(ChunkUnloadEvent e) {
        if (!plugin.lock().hasLock()) {
            return;
        }
        plugin.dirty().markChunk(e.getWorld(), e.getChunk(), plugin.serverRoot());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSave(WorldSaveEvent e) {
        if (!plugin.lock().hasLock()) {
            return;
        }
        try {
            plugin.dirty().mark(PathPolicy.relative(plugin.serverRoot(), e.getWorld().getWorldFolder().toPath().resolve("level.dat")));
        } catch (IllegalArgumentException ignored) {
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        if (!plugin.lock().hasLock()) {
            return;
        }
        plugin.dirty().markPlayer(e.getPlayer(), plugin.serverRoot(), plugin.serverRoot());
    }
}
