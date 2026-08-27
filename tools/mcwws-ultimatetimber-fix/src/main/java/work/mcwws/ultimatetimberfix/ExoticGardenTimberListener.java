package work.mcwws.ultimatetimberfix;

import com.songoda.ultimatetimber.events.TreeFallEvent;
import com.songoda.ultimatetimber.events.TreeFellEvent;
import com.songoda.ultimatetimber.tree.DetectedTree;
import com.songoda.ultimatetimber.tree.ITreeBlock;
import com.songoda.ultimatetimber.tree.TreeBlockSet;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class ExoticGardenTimberListener implements Listener {

    private final McwwsUltimateTimberFixPlugin plugin;

    public ExoticGardenTimberListener(McwwsUltimateTimberFixPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTreeFall(TreeFallEvent event) {
        Player player = event.getPlayer();
        if (plugin.isBypassed(player)) {
            return;
        }

        DetectedTree tree = event.getDetectedTree();
        String saplingId = resolveSaplingId(tree);
        if (saplingId == null) {
            return;
        }

        Location base = getBaseLocation(tree);
        if (base == null) {
            return;
        }

        Set<String> blockKeys = new LinkedHashSet<>();
        Set<String> leafKeys = new LinkedHashSet<>();
        collectTreeKeys(tree, blockKeys, leafKeys);

        long expiresAt = System.currentTimeMillis() + plugin.sessionTimeoutMs();
        FellSession session = new FellSession(
                player != null ? player.getUniqueId() : new UUID(0L, 0L),
                saplingId,
                base,
                blockKeys,
                leafKeys,
                expiresAt
        );
        plugin.getSessionManager().start(session);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreeFell(TreeFellEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        FellSession session = plugin.getSessionManager().get(player.getUniqueId());
        if (session == null) {
            return;
        }

        ExoticGardenSaplingHelper.dropSaplingsForLeaves(plugin, session, player);
        ExoticGardenSaplingHelper.scheduleReplant(plugin, session);
        plugin.getFootprintStore().removeFootprint(session.getBaseLocation());
        plugin.getSessionManager().endDelayed(player.getUniqueId(), plugin.sessionCleanupDelayTicks(), plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        FellSession session = plugin.getSessionManager().findAt(item.getLocation());
        if (session == null) {
            return;
        }

        ItemStack stack = item.getItemStack();
        if (ExoticGardenSaplingHelper.isLeafOrVanillaSapling(stack.getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallingBlockLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) {
            return;
        }

        FellSession session = plugin.getSessionManager().findAt(event.getBlock().getLocation());
        if (session == null) {
            return;
        }

        if (ExoticGardenSaplingHelper.isLeafOrVanillaSapling(event.getTo())) {
            event.setCancelled(true);
        }
    }

    private String resolveSaplingId(DetectedTree tree) {
        String saplingId = SlimefunTreeDetector.resolveSaplingId(tree);
        if (saplingId != null) {
            return saplingId;
        }
        TreeFootprintStore store = plugin.getFootprintStore();
        if (store == null) {
            return null;
        }
        return store.resolveSaplingId(tree);
    }

    private static Location getBaseLocation(DetectedTree tree) {
        TreeBlockSet<?> blocks = tree.getDetectedTreeBlocks();
        if (blocks == null || blocks.getInitialLogBlock() == null) {
            return null;
        }
        Object raw = blocks.getInitialLogBlock().getBlock();
        if (raw instanceof Block block) {
            return block.getLocation();
        }
        return null;
    }

    private static void collectTreeKeys(DetectedTree tree, Set<String> blockKeys, Set<String> leafKeys) {
        for (ITreeBlock<?> treeBlock : tree.getDetectedTreeBlocks().getAllTreeBlocks()) {
            Object raw = treeBlock.getBlock();
            if (!(raw instanceof Block block)) {
                continue;
            }
            String key = TreeFootprintStore.key(block.getLocation());
            blockKeys.add(key);
            if (SlimefunTreeDetector.isLeafBlock(treeBlock)) {
                leafKeys.add(key);
            }
        }
    }
}
