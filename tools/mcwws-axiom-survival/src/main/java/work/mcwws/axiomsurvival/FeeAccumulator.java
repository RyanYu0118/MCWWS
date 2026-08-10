package work.mcwws.axiomsurvival;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
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
            Map<String, Long> placedCounts
    ) {
        public double total() {
            return demolition + material + labor;
        }

        public static Result empty() {
            return new Result(0D, 0D, 0D, 0L, 0L, Map.of(), Map.of());
        }
    }

    public static final class Builder {
        private final PriceCatalog prices;
        private final LaborRates laborRates;
        private double demolition;
        private double material;
        private double labor;
        private long affectedBlocks;
        long protectedBlocks;
        private final Map<String, Long> removedCounts = new HashMap<>();
        private final Map<String, Long> placedCounts = new HashMap<>();

        public Builder(PriceCatalog prices, LaborRates laborRates) {
            this.prices = prices;
            this.laborRates = laborRates;
        }

        public void addProtected() {
            protectedBlocks++;
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
                    Map.copyOf(placedCounts)
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
