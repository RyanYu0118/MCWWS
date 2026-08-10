package work.mcwws.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.fastasyncworldedit.core.util.MaskTraverser;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class FeeEstimate {

    /**
     * 劳务费按格数计价，与材料市价无关。
     * 默认：放置 0.5 / 格，拆除 = 放置 × 2。
     */
    public record LaborRates(double placeUnit, double demolishUnit) {
        public static LaborRates defaults() {
            return new LaborRates(0.5D, 1.0D);
        }
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
    }

    private FeeEstimate() {
    }

    public static Result empty() {
        return new Result(0D, 0D, 0D, 0L, 0L, Map.of(), Map.of());
    }

    public static Result forSet(PriceCatalog prices, LaborRates laborRates, Region region, World world, String patternInput, com.sk89q.worldedit.extension.platform.Actor actor) throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(world);
        Pattern pattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(patternInput, context);
        FaweRegionSync.flushBeforeEstimate(world, region);
        ResultBuilder builder = new ResultBuilder(prices, laborRates);
        Extent counter = EstimateCountExtent.forEstimate(world, builder);
        for (BlockVector3 pos : region) {
            try {
                pattern.apply(counter, pos, pos);
            } catch (WorldEditException ex) {
                throw new InputParseException(ex.getMessage());
            }
        }
        return builder.build();
    }

    public static Result forReplace(PriceCatalog prices, LaborRates laborRates, Region region, World world, String fromInput, String toInput, com.sk89q.worldedit.extension.platform.Actor actor) throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(world);
        FaweRegionSync.flushBeforeEstimate(world, region);
        Extent snapshot = BukkitSnapshotExtent.forEstimate(world);
        Mask fromMask;
        if (fromInput == null || fromInput.isBlank()) {
            fromMask = new ExistingBlockMask(snapshot);
        } else {
            fromMask = WorldEdit.getInstance().getMaskFactory().parseFromInput(fromInput, context);
            new MaskTraverser(fromMask).setNewExtent(snapshot);
        }
        Pattern toPattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(toInput, context);
        ResultBuilder builder = new ResultBuilder(prices, laborRates);
        Extent counter = EstimateCountExtent.forEstimate(world, builder);
        for (BlockVector3 pos : region) {
            if (!fromMask.test(pos)) {
                continue;
            }
            try {
                toPattern.apply(counter, pos, pos);
            } catch (WorldEditException ex) {
                throw new InputParseException(ex.getMessage());
            }
        }
        return builder.build();
    }

    /**
     * 与 //replacenear 一致：以玩家脚下方块为中心、半径为 size 的立方体（非选区）。
     */
    public static Result forReplaceNear(PriceCatalog prices, LaborRates laborRates, World world, BlockVector3 center, int radius, String fromInput, String toInput, com.sk89q.worldedit.extension.platform.Actor actor) throws InputParseException {
        if (radius < 0) {
            throw new InputParseException("半径不能为负数");
        }
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(world);
        FaweRegionSync.flushBeforeEstimate(world, center, radius);
        Extent snapshot = BukkitSnapshotExtent.forEstimate(world);
        Mask fromMask;
        if (fromInput == null || fromInput.isBlank()) {
            fromMask = new ExistingBlockMask(snapshot);
        } else {
            fromMask = WorldEdit.getInstance().getMaskFactory().parseFromInput(fromInput, context);
            new MaskTraverser(fromMask).setNewExtent(snapshot);
        }
        Pattern toPattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(toInput, context);
        ResultBuilder builder = new ResultBuilder(prices, laborRates);
        Extent counter = EstimateCountExtent.forEstimate(world, builder);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockVector3 pos = center.add(dx, dy, dz);
                    if (!fromMask.test(pos)) {
                        continue;
                    }
                    try {
                        toPattern.apply(counter, pos, pos);
                    } catch (WorldEditException ex) {
                        throw new InputParseException(ex.getMessage());
                    }
                }
            }
        }
        return builder.build();
    }

    public static long replaceNearScanVolume(int radius) {
        if (radius < 0) {
            return 0L;
        }
        long edge = 2L * radius + 1L;
        return edge * edge * edge;
    }

    public static Result forUniformPattern(PriceCatalog prices, LaborRates laborRates, Region region, World world, String patternInput, com.sk89q.worldedit.extension.platform.Actor actor) throws InputParseException {
        return forSet(prices, laborRates, region, world, patternInput, actor);
    }

    /**
     * //stack：按选区内容复制 count 次，计费规则与 //set 相同（拆除目标格 + 材料 + 劳务）。
     */
    public static Result forStack(PriceCatalog prices, LaborRates laborRates, Region region, World world, StackCommandArgs stackArgs) {
        BlockVector3 blockOffset = stackArgs.blockOffset(region);
        FaweRegionSync.flushBeforeEstimate(world, region);
        RegionChunkLoader.ensureLoadedForStack(world, region, blockOffset, stackArgs.count);
        ResultBuilder builder = new ResultBuilder(prices, laborRates);
        Extent counter = EstimateCountExtent.forEstimate(world, builder);
        for (int repetition = 1; repetition <= stackArgs.count; repetition++) {
            BlockVector3 translation = blockOffset.multiply(repetition);
            for (BlockVector3 pos : region) {
                BaseBlock source = BukkitSnapshotExtent.readBlock(world, pos).toBaseBlock();
                if (stackArgs.ignoreAir && source.getBlockType() == BlockTypes.AIR) {
                    continue;
                }
                BlockVector3 dest = pos.add(translation);
                counter.setBlock(dest, source);
            }
        }
        return builder.build();
    }

    public static String itemIdFromBaseBlock(BaseBlock block) {
        if (block == null || block.getBlockType() == BlockTypes.AIR) {
            return "air";
        }
        return PriceCatalog.normalize(block.getBlockType().id());
    }

    public static String itemIdFromState(BlockState state) {
        if (state == null) {
            return "";
        }
        return itemIdFromBaseBlock(state.toBaseBlock());
    }

    static final class ResultBuilder {
        private final PriceCatalog prices;
        private final LaborRates laborRates;
        private double demolition;
        private double material;
        private double labor;
        private long affectedBlocks;
        long protectedBlocks;
        private final Map<String, Long> removedCounts = new HashMap<>();
        private final Map<String, Long> placedCounts = new HashMap<>();

        ResultBuilder(PriceCatalog prices, LaborRates laborRates) {
            this.prices = prices;
            this.laborRates = laborRates != null ? laborRates : LaborRates.defaults();
        }

        void addChange(BaseBlock existing, BaseBlock target) {
            if (existing == null || target == null) {
                return;
            }
            if (existing.equals(target)) {
                return;
            }
            accumulate(existing, target);
        }

        /** //replace：匹配 from 且目标与现有不同才计费（与 FAWE 实际改块对齐） */
        void addReplaceChange(BaseBlock existing, BaseBlock target) {
            if (existing == null || target == null || existing.equals(target)) {
                return;
            }
            accumulate(existing, target);
        }

        private void accumulate(BaseBlock existing, BaseBlock target) {
            String oldId = itemIdFromBaseBlock(existing);
            String newId = itemIdFromBaseBlock(target);
            if (!"air".equals(oldId)) {
                // 拆除材料项仍按市价；劳务按拆除格数计费（与市价无关）
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

        Result build() {
            demolition = round(demolition);
            material = round(material);
            labor = round(labor);
            return new Result(
                    demolition,
                    material,
                    labor,
                    affectedBlocks,
                    protectedBlocks,
                    Map.copyOf(removedCounts),
                    Map.copyOf(placedCounts)
            );
        }
    }

    public static double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    public static String rootCommand(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("//")) {
            trimmed = trimmed.substring(2);
        } else if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = trimmed.indexOf(' ');
        String head = space >= 0 ? trimmed.substring(0, space) : trimmed;
        return head.toLowerCase(Locale.ROOT);
    }

    public static String[] splitArgs(String raw) {
        if (raw == null) {
            return new String[0];
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("//")) {
            trimmed = trimmed.substring(2);
        } else if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new String[0];
        }
        return trimmed.substring(space + 1).trim().split("\\s+");
    }

    public static long regionVolume(Region region) {
        long count = 0L;
        for (BlockVector3 ignored : region) {
            count++;
        }
        return count;
    }
}
