package work.mcwws.axiomsurvival;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FeeAccumulator {

    public record LaborRates(double placeUnit, double demolishUnit) {
    }

    public record Result(
            double demolition,
            double material,
            double labor,
            long affectedBlocks,
            long protectedBlocks,
            Map<String, Long> removedCounts,
            Map<String, Long> placedCounts,
            List<BlockState> protectedStates
    ) {
        public double total() {
            return demolition + material + labor;
        }

        public static Result empty() {
            return new Result(0D, 0D, 0D, 0L, 0L, Map.of(), Map.of(), List.of());
        }
    }

    public static final class Builder {
        private final PriceCatalog prices;
        private final LaborRates laborRates;
        private final int protectedCaptureCap;
        private double demolition;
        private double material;
        private double labor;
        private long affectedBlocks;
        long protectedBlocks;
        private final Map<String, Long> removedCounts = new HashMap<>();
        private final Map<String, Long> placedCounts = new HashMap<>();
        private final List<BlockState> protectedStates = new ArrayList<>();

        public Builder(PriceCatalog prices, LaborRates laborRates, int protectedCaptureCap) {
            this.prices = prices;
            this.laborRates = laborRates;
            this.protectedCaptureCap = Math.max(protectedCaptureCap, 0);
        }

        /** 受保护方块不计费；同时留下原状快照，Axiom 写入后由 ProtectedBlockGuard 还原 */
        public void addProtected(Block block) {
            protectedBlocks++;
            if (block != null && protectedStates.size() < protectedCaptureCap) {
                protectedStates.add(block.getState());
            }
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
                demolition += prices.getBuyPrice(oldId);
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
            return new Result(
                    round(demolition),
                    round(material),
                    round(labor),
                    affectedBlocks,
                    protectedBlocks,
                    Map.copyOf(removedCounts),
                    Map.copyOf(placedCounts),
                    List.copyOf(protectedStates)
            );
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
