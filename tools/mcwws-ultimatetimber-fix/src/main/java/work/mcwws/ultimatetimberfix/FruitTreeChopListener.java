package work.mcwws.ultimatetimberfix;

import com.songoda.ultimatetimber.UltimateTimber;
import com.songoda.ultimatetimber.tree.DetectedTree;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * UltimateTimber 经常无法把 ExoticGarden 果树识别为可伐树木（schematic 结构 + 果实头颅）。
 * 当 UT 检测失败时，由此监听器直接连根砍果树。
 */
public final class FruitTreeChopListener implements Listener {

    /** ExoticGarden 的树是 schematic 拼出来的，枝干与树叶常常只在斜向相邻，六面 BFS 会漏掉大半棵树。 */
    private static final int[][] NEIGHBOURS = buildNeighbours();

    private static int[][] buildNeighbours() {
        int[][] offsets = new int[26][];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    offsets[i++] = new int[] {dx, dy, dz};
                }
            }
        }
        return offsets;
    }

    private final McwwsUltimateTimberFixPlugin plugin;

    public FruitTreeChopListener(McwwsUltimateTimberFixPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFruitTreeChop(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.isBypassed(player)) {
            plugin.debug("跳过：玩家持有 bypass 权限");
            return;
        }

        Block broken = event.getBlock();
        if (!Tag.LOGS.isTagged(broken.getType())) {
            return;
        }

        if (!looksLikeFruitTree(broken)) {
            plugin.debug("非果树原木 @" + format(broken.getLocation()) + " type=" + broken.getType());
            return;
        }

        // 若 UltimateTimber 自己能识别，交给它（保留动画）
        if (ultimateTimberCanDetect(broken)) {
            plugin.debug("UltimateTimber 已识别该树，交由其处理 @" + format(broken.getLocation()));
            return;
        }

        String saplingId = resolveSaplingIdNear(broken);
        if (saplingId == null) {
            plugin.getLogger().warning("识别到疑似果树但无法解析树苗 ID @" + format(broken.getLocation()));
            return;
        }

        TreeScan scan = scanTree(broken);
        plugin.debug("果树扫描 sapling=" + saplingId
                + " logs=" + scan.logs.size()
                + " leaves=" + scan.leaves.size()
                + " heads=" + scan.heads.size()
                + " @" + format(broken.getLocation()));

        if (scan.logs.isEmpty()) {
            return;
        }
        // 至少要有树叶或果实头，避免误伤普通原木柱
        if (scan.leaves.isEmpty() && scan.heads.isEmpty() && scan.logs.size() < 2) {
            plugin.debug("跳过：缺少树叶/果实头且原木过少");
            return;
        }

        event.setCancelled(true);

        boolean creative = player != null && player.getGameMode() == GameMode.CREATIVE;
        Location base = findBaseLog(scan.logs).getLocation();

        Set<String> leafKeys = new LinkedHashSet<>();
        for (Block leaf : scan.leaves) {
            leafKeys.add(TreeFootprintStore.key(leaf.getLocation()));
        }

        // 先处理树叶：不掉叶方块，按概率掉果树苗
        Random random = ThreadLocalRandom.current();
        double chance = plugin.saplingDropChancePerLeaf() / 100.0D;
        ItemStack saplingDrop = ExoticGardenSaplingHelper.createSaplingItem(saplingId);
        for (Block leaf : scan.leaves) {
            clearSlimefun(leaf);
            leaf.setType(Material.AIR, false);
            if (!creative && saplingDrop != null && random.nextDouble() <= chance) {
                leaf.getWorld().dropItemNaturally(leaf.getLocation().add(0.5, 0.5, 0.5), saplingDrop.clone());
            }
        }

        // 果实头：掉落对应果实物品
        for (Block head : scan.heads) {
            String fruitId = SlimefunTreeDetector.slimefunIdAt(head);
            ItemStack fruit = null;
            if (fruitId != null) {
                SlimefunItem item = SlimefunItem.getById(fruitId);
                if (item != null) {
                    fruit = item.getItem().clone();
                }
            }
            clearSlimefun(head);
            head.setType(Material.AIR, false);
            if (!creative && fruit != null) {
                head.getWorld().dropItemNaturally(head.getLocation().add(0.5, 0.5, 0.5), fruit);
            }
        }

        // 原木：正常掉落
        for (Block log : scan.logs) {
            Material type = log.getType();
            clearSlimefun(log);
            log.setType(Material.AIR, false);
            if (!creative && Tag.LOGS.isTagged(type)) {
                log.getWorld().dropItemNaturally(log.getLocation().add(0.5, 0.5, 0.5), new ItemStack(type));
            }
        }

        plugin.getFootprintStore().removeFootprint(base);

        FellSession session = new FellSession(
                player != null ? player.getUniqueId() : new java.util.UUID(0L, 0L),
                saplingId,
                base,
                Set.of(TreeFootprintStore.key(base)),
                leafKeys,
                System.currentTimeMillis() + plugin.sessionTimeoutMs()
        );
        ExoticGardenSaplingHelper.scheduleReplant(plugin, session);

        plugin.getLogger().info("已兜底连根砍 ExoticGarden 果树 sapling=" + saplingId
                + " logs=" + scan.logs.size()
                + " leaves=" + scan.leaves.size()
                + " heads=" + scan.heads.size()
                + " @" + format(base));
    }

    private boolean ultimateTimberCanDetect(Block broken) {
        try {
            UltimateTimber ut = UltimateTimber.getInstance();
            if (ut == null) {
                return false;
            }
            DetectedTree tree = ut.getTreeDetectionManager().detectTree(broken);
            return tree != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean looksLikeFruitTree(Block origin) {
        TreeFootprintStore store = plugin.getFootprintStore();
        if (store.contains(origin.getLocation()) || store.getSaplingId(origin.getLocation()) != null) {
            return true;
        }
        if (SlimefunTreeDetector.isFruitTreeBlock(origin)) {
            return true;
        }
        return hasNearbyFruitFeature(origin);
    }

    private boolean hasNearbyFruitFeature(Block origin) {
        TreeFootprintStore store = plugin.getFootprintStore();
        int radius = 5;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = 0; dy <= 10; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
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

    private String resolveSaplingIdNear(Block origin) {
        TreeFootprintStore store = plugin.getFootprintStore();
        String fromStore = store.getSaplingId(origin.getLocation());
        if (fromStore != null) {
            return fromStore;
        }
        String direct = SlimefunTreeDetector.resolveSaplingId(origin);
        if (direct != null) {
            return direct;
        }

        int radius = 6;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = 0; dy <= 12; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block relative = origin.getRelative(dx, dy, dz);
                    String id = store.getSaplingId(relative.getLocation());
                    if (id != null) {
                        return id;
                    }
                    id = SlimefunTreeDetector.resolveSaplingId(relative);
                    if (id != null) {
                        return id;
                    }
                }
            }
        }
        return null;
    }

    private static final int LEAF_RADIUS = 4;

    private TreeScan scanTree(Block origin) {
        TreeScan scan = new TreeScan();
        int max = plugin.footprintMaxBlocks();

        // 第一步：只沿原木做 BFS，得到主干与枝条
        Queue<Block> queue = new ArrayDeque<>();
        Set<String> logKeys = new HashSet<>();
        queue.add(origin);
        while (!queue.isEmpty() && scan.logs.size() < max) {
            Block block = queue.poll();
            if (!Tag.LOGS.isTagged(block.getType())) {
                continue;
            }
            if (!logKeys.add(TreeFootprintStore.key(block.getLocation()))) {
                continue;
            }
            scan.logs.add(block);
            for (int[] offset : NEIGHBOURS) {
                queue.add(block.getRelative(offset[0], offset[1], offset[2]));
            }
        }

        // 第二步：只在原木邻域内收树叶与果实头，避免顺着树叶串到隔壁森林
        Set<String> claimed = new HashSet<>();
        for (Block log : scan.logs) {
            for (int dx = -LEAF_RADIUS; dx <= LEAF_RADIUS; dx++) {
                for (int dy = -LEAF_RADIUS; dy <= LEAF_RADIUS; dy++) {
                    for (int dz = -LEAF_RADIUS; dz <= LEAF_RADIUS; dz++) {
                        Block block = log.getRelative(dx, dy, dz);
                        Material type = block.getType();
                        boolean leaf = Tag.LEAVES.isTagged(type);
                        boolean head = type == Material.PLAYER_HEAD || type == Material.PLAYER_WALL_HEAD;
                        if (!leaf && !head) {
                            continue;
                        }
                        if (head && !SlimefunTreeDetector.isFruitTreeBlock(block)
                                && SlimefunTreeDetector.slimefunIdAt(block) == null) {
                            // 别吞掉普通装饰头
                            continue;
                        }
                        if (!claimed.add(TreeFootprintStore.key(block.getLocation()))) {
                            continue;
                        }
                        if (leaf) {
                            scan.leaves.add(block);
                        } else {
                            scan.heads.add(block);
                        }
                    }
                }
            }
        }
        return scan;
    }

    private static Block findBaseLog(List<Block> logs) {
        Block base = logs.get(0);
        for (Block log : logs) {
            if (log.getY() < base.getY()) {
                base = log;
            }
        }
        return base;
    }

    private static void clearSlimefun(Block block) {
        try {
            if (BlockStorage.hasBlockInfo(block)) {
                BlockStorage.clearBlockInfo(block);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String format(Location location) {
        return location.getWorld().getName()
                + ' ' + location.getBlockX()
                + ' ' + location.getBlockY()
                + ' ' + location.getBlockZ();
    }

    private static final class TreeScan {
        private final List<Block> logs = new ArrayList<>();
        private final List<Block> leaves = new ArrayList<>();
        private final List<Block> heads = new ArrayList<>();
    }
}
