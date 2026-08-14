package work.mcwws.axiomsurvival;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FeeAccumulator {

    public record LaborRates(double placeUnit, double demolishUnit) {
    }

    /**
     * @param removedCounts    原始拆除计数，撤销配对靠它，不做搬运对冲
     * @param placedCounts     原始放置计数，同上
     * @param netRemovedCounts 对冲搬运后真正流入市场的量
     * @param netPlacedCounts  对冲搬运后真正从市场取出的量
     * @param movedBlocks      被判定为搬运（原地拆、别处放同种方块）的格数，只收劳务费
     * @param minDistance      本次处理的方块（含受保护）离玩家的最小距离；未知时为 {@link #UNKNOWN_DISTANCE}
     */
    public record Result(
            double salvage,
            double material,
            double labor,
            long affectedBlocks,
            long protectedBlocks,
            long residenceDeniedBlocks,
            long movedBlocks,
            Map<String, Long> removedCounts,
            Map<String, Long> placedCounts,
            Map<String, Long> netRemovedCounts,
            Map<String, Long> netPlacedCounts,
            List<BlockState> protectedStates,
            double minDistance
    ) {
        /**
         * 材料 + 人工 − 回收。拆下来的方块按卖价折现给玩家，所以净额可以为负，
         * 负数代表这笔编辑倒赚，由 ChargeService 走存款。
         */
        public double total() {
            return material + labor - salvage;
        }

        public static Result empty() {
            return new Result(0D, 0D, 0D, 0L, 0L, 0L, 0L, Map.of(), Map.of(), Map.of(), Map.of(), List.of(), UNKNOWN_DISTANCE);
        }
    }

    /** 没有采到任何坐标时用这个，提示里显示为 — */
    public static final double UNKNOWN_DISTANCE = Double.POSITIVE_INFINITY;

    public static final class Builder {
        private final PriceCatalog prices;
        private final LaborRates laborRates;
        private final double salvageRate;
        private final boolean netMoves;
        private final int protectedCaptureCap;
        private double salvage;
        private double material;
        private double labor;
        private long affectedBlocks;
        long protectedBlocks;
        long residenceDeniedBlocks;
        private final Map<String, Long> removedCounts = new HashMap<>();
        private final Map<String, Long> placedCounts = new HashMap<>();
        private final List<BlockState> protectedStates = new ArrayList<>();
        private Player player;
        private boolean hasOrigin;
        private double originX;
        private double originY;
        private double originZ;
        private double minDistance = UNKNOWN_DISTANCE;

        public Builder(
                PriceCatalog prices,
                LaborRates laborRates,
                double salvageRate,
                boolean netMoves,
                int protectedCaptureCap
        ) {
            this.prices = prices;
            this.laborRates = laborRates;
            this.salvageRate = Math.min(Math.max(salvageRate, 0D), 1D);
            this.netMoves = netMoves;
            this.protectedCaptureCap = Math.max(protectedCaptureCap, 0);
        }

        public Builder origin(Player player) {
            if (player == null) {
                return this;
            }
            this.player = player;
            Location location = player.getLocation();
            this.originX = location.getX();
            this.originY = location.getY();
            this.originZ = location.getZ();
            this.hasOrigin = true;
            return this;
        }

        void offerBlock(int x, int y, int z) {
            offerPoint(x + 0.5D, y + 0.5D, z + 0.5D);
        }

        void offerPoint(double x, double y, double z) {
            if (!hasOrigin) {
                return;
            }
            double dx = x - originX;
            double dy = y - originY;
            double dz = z - originZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }

        /** 受保护方块不计费；同时留下原状快照，Axiom 写入后由 ProtectedBlockGuard 还原 */
        public void addProtected(Block block) {
            protectedBlocks++;
            if (block != null) {
                offerBlock(block.getX(), block.getY(), block.getZ());
                if (protectedStates.size() < protectedCaptureCap) {
                    protectedStates.add(block.getState());
                }
            }
        }

        public void addChange(Block block, BlockData target) {
            if (block == null || target == null) {
                return;
            }
            BlockData existing = block.getBlockData();
            if (existing.matches(target)) {
                return;
            }
            if (!ResidenceProtection.canChange(player, block, target)) {
                residenceDeniedBlocks++;
                offerBlock(block.getX(), block.getY(), block.getZ());
                return;
            }
            offerBlock(block.getX(), block.getY(), block.getZ());
            accumulate(existing, target);
        }

        public void addChange(BlockData existing, BlockData target) {
            if (existing == null || target == null) {
                return;
            }
            if (existing.matches(target)) {
                return;
            }
            accumulate(existing, target);
        }

        private void accumulate(BlockData existing, BlockData target) {
            String oldId = itemIdFromBlockData(existing);
            String newId = itemIdFromBlockData(target);
            if (!"air".equals(oldId)) {
                // 拆下来的方块进市场（MarketBridge 记 sell 侧库存），这里按卖价折现给玩家
                salvage += prices.getSellPrice(oldId) * salvageRate;
                removedCounts.merge(oldId, 1L, Long::sum);
                labor += laborRates.demolishUnit();
            }
            if (!"air".equals(newId)) {
                material += prices.getBuyPrice(newId);
                placedCounts.merge(newId, 1L, Long::sum);
                labor += laborRates.placeUnit();
            }
            affectedBlocks++;
        }

        public Result build() {
            Map<String, Long> netRemoved = new HashMap<>(removedCounts);
            Map<String, Long> netPlaced = new HashMap<>(placedCounts);
            double netMaterial = material;
            double netSalvage = salvage;
            long moved = 0L;
            if (netMoves) {
                for (Map.Entry<String, Long> entry : removedCounts.entrySet()) {
                    String id = entry.getKey();
                    long placedSame = placedCounts.getOrDefault(id, 0L);
                    if (placedSame <= 0L) {
                        continue;
                    }
                    // 同种方块「这里拆掉、那里放下」就是搬运：材料没消耗、市场也没进出，只该收劳务费
                    long count = Math.min(entry.getValue(), placedSame);
                    moved += count;
                    netMaterial -= prices.getBuyPrice(id) * count;
                    netSalvage -= prices.getSellPrice(id) * salvageRate * count;
                    subtract(netRemoved, id, count);
                    subtract(netPlaced, id, count);
                }
            }
            return new Result(
                    // 物价表可能在预估途中重载，减出来的极小负值直接抹平
                    Math.max(round(netSalvage), 0D),
                    Math.max(round(netMaterial), 0D),
                    round(labor),
                    affectedBlocks,
                    protectedBlocks,
                    residenceDeniedBlocks,
                    moved,
                    Map.copyOf(removedCounts),
                    Map.copyOf(placedCounts),
                    Map.copyOf(netRemoved),
                    Map.copyOf(netPlaced),
                    List.copyOf(protectedStates),
                    minDistance
            );
        }

        private static void subtract(Map<String, Long> counts, String id, long amount) {
            Long current = counts.get(id);
            if (current == null) {
                return;
            }
            long left = current - amount;
            if (left > 0L) {
                counts.put(id, left);
            } else {
                counts.remove(id);
            }
        }

        private static String itemIdFromBlockData(BlockData blockData) {
            if (blockData == null) {
                return "air";
            }
            Material material = blockData.getMaterial();
            if (material.isAir()) {
                return "air";
            }
            return PriceCatalog.normalize(material.name().toLowerCase());
        }

        private static double round(double value) {
            return Math.round(value * 100D) / 100D;
        }
    }

    public static double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    public static boolean hasDistance(double distance) {
        return Double.isFinite(distance) && distance >= 0D;
    }

    public static double minDistance(double left, double right) {
        if (!hasDistance(left)) {
            return right;
        }
        if (!hasDistance(right)) {
            return left;
        }
        return Math.min(left, right);
    }

    public static String formatDistance(double distance) {
        if (!hasDistance(distance)) {
            return "—";
        }
        return String.format(Locale.ROOT, "%.1f", distance);
    }

    /** 在已有占位符后面追加 {near}，供扣费/拒绝提示使用 */
    public static String[] withNear(double minDistance, String... replacements) {
        String[] source = replacements == null ? new String[0] : replacements;
        String[] out = Arrays.copyOf(source, source.length + 2);
        out[source.length] = "near";
        out[source.length + 1] = formatDistance(minDistance);
        return out;
    }

    static String itemIdFromBlockData(BlockData blockData) {
        if (blockData == null) {
            return "air";
        }
        Material material = blockData.getMaterial();
        if (material.isAir()) {
            return "air";
        }
        return PriceCatalog.normalize(material.name().toLowerCase());
    }

    private FeeAccumulator() {
    }
}
