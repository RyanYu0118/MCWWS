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
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.HashMap;
import java.util.LinkedHashMap;
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

    /**
     * @param removedCounts    原始拆除计数，不做搬运对冲
     * @param placedCounts     原始放置计数，同上
     * @param netRemovedCounts 对冲搬运后真正流入市场的量
     * @param netPlacedCounts  对冲搬运后真正从市场取出的量
     * @param movedBlocks      被判定为搬运（原地拆、别处放同种方块）的格数，只收劳务费
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
            Map<String, Long> netPlacedCounts
    ) {
        /**
         * 材料 + 人工 − 回收。拆下来的方块按卖价折现给玩家，所以净额可以为负，
         * 负数代表这条指令倒赚，由监听器走存款。
         */
        public double total() {
            return material + labor - salvage;
        }
    }

    private FeeEstimate() {
    }

    public static Result empty() {
        return new Result(0D, 0D, 0D, 0L, 0L, 0L, 0L, Map.of(), Map.of(), Map.of(), Map.of());
    }

    public static Result forSet(PriceCatalog prices, LaborRates laborRates, Region region, World world, String patternInput, com.sk89q.worldedit.extension.platform.Actor actor) throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(world);
        Pattern pattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(patternInput, context);
        FaweRegionSync.flushBeforeEstimate(world, region);
        EstimateContext.setRegion(region);
        ResultBuilder builder = new ResultBuilder(prices, laborRates);
        Extent counter = EstimateCountExtent.forEstimate(world, builder);
        for (BlockVector3 pos : region) {
            BaseBlock target = pattern.applyBlock(pos);
            counter.setBlock(pos, target);
        }
        return builder.build();
    }

    public static Result forReplace(PriceCatalog prices, LaborRates laborRates, Region region, World world, String fromInput, String toInput, com.sk89q.worldedit.extension.platform.Actor actor) throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(world);
        FaweRegionSync.flushBeforeEstimate(world, region);
        EstimateContext.setRegion(region);
        Pattern fromPattern = null;
        Extent snapshot;
        Mask fromMask;
        if (fromInput == null || fromInput.isBlank()) {
            snapshot = BukkitSnapshotExtent.forEstimate(world);
            fromMask = new ExistingBlockMask(snapshot);
        } else {
            fromPattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(fromInput, context);
            snapshot = EstimateFromSnapshotExtent.forReplaceFrom(world, fromPattern);
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
            BaseBlock target = toPattern.applyBlock(pos);
            counter.setBlock(pos, target);
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
        EstimateContext.setRegion(new CuboidRegion(
                center.add(-radius, -radius, -radius),
                center.add(radius, radius, radius)
        ));
        Pattern fromPattern = null;
        Extent snapshot;
        Mask fromMask;
        if (fromInput == null || fromInput.isBlank()) {
            snapshot = BukkitSnapshotExtent.forEstimate(world);
            fromMask = new ExistingBlockMask(snapshot);
        } else {
            fromPattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(fromInput, context);
            snapshot = EstimateFromSnapshotExtent.forReplaceFrom(world, fromPattern);
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
                    BaseBlock target = toPattern.applyBlock(pos);
                    counter.setBlock(pos, target);
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
        EstimateContext.setRegion(region);
        RegionChunkLoader.ensureLoadedForStack(world, region, blockOffset, stackArgs.count);
        ResultBuilder builder = new ResultBuilder(prices, laborRates);
        Extent counter = EstimateCountExtent.forEstimate(world, builder);
        for (int repetition = 1; repetition <= stackArgs.count; repetition++) {
            BlockVector3 translation = blockOffset.multiply(repetition);
            for (BlockVector3 raw : region) {
                BlockVector3 pos = freeze(raw);
                BaseBlock source = BukkitSnapshotExtent.readBlock(world, pos).toBaseBlock();
                if (stackArgs.ignoreAir && isAirBlock(source)) {
                    continue;
                }
                counter.setBlock(pos.add(translation), source);
            }
        }
        return builder.build();
    }

    /**
     * //move：源位置留下 leave（默认空气），目标位置放下原来的方块。
     * 对账必须用「移动前快照 vs 最终状态」，不能再走 {@link EstimateCountExtent} 现场读世界：
     * 读块器在「写成空气」时可能把源格看成已是空气，拆除进不了账，搬运对冲失败，就会误收材料费。
     */
    public static Result forMove(
            PriceCatalog prices,
            LaborRates laborRates,
            Region region,
            World world,
            BlockVector3 offset,
            String leaveInput,
            boolean copyAir,
            String maskInput,
            com.sk89q.worldedit.extension.platform.Actor actor
    ) throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(world);
        String leavePatternInput = leaveInput == null || leaveInput.isBlank() ? "air" : leaveInput.trim();
        boolean leaveIsAir = "air".equalsIgnoreCase(leavePatternInput);
        Pattern leave = leaveIsAir
                ? null
                : WorldEdit.getInstance().getPatternFactory().parseFromInput(leavePatternInput, context);
        Mask mask = null;
        if (maskInput != null && !maskInput.isBlank()) {
            mask = WorldEdit.getInstance().getMaskFactory().parseFromInput(maskInput, context);
            new MaskTraverser(mask).setNewExtent(BukkitSnapshotExtent.forEstimate(world));
        }
        FaweRegionSync.flushBeforeEstimate(world, region);
        EstimateContext.setRegion(region);
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        RegionChunkLoader.ensureLoaded(world, min, max);
        RegionChunkLoader.ensureLoaded(world, min.add(offset), max.add(offset));

        Map<BlockVector3, BaseBlock> original = new LinkedHashMap<>();
        Map<BlockVector3, BaseBlock> copies = new LinkedHashMap<>();
        for (BlockVector3 raw : region) {
            // FAWE 选区迭代器复用 MutableBlockVector3，add() 会改原地坐标。必须先冻成不可变点再当 Map 键。
            BlockVector3 pos = freeze(raw);
            BaseBlock source = BukkitSnapshotExtent.readBlock(world, pos).toBaseBlock();
            original.put(pos, source);
            original.computeIfAbsent(pos.add(offset), dest -> BukkitSnapshotExtent.readBlock(world, dest).toBaseBlock());
            if (mask != null && !mask.test(pos)) {
                continue;
            }
            if (!copyAir && isAirBlock(source)) {
                continue;
            }
            copies.put(pos, source);
        }
        Map<BlockVector3, BaseBlock> finals = new LinkedHashMap<>(original);
        for (BlockVector3 sourcePos : copies.keySet()) {
            BaseBlock left = leaveIsAir
                    ? BlockTypes.AIR.getDefaultState().toBaseBlock()
                    : leave.applyBlock(sourcePos);
            finals.put(sourcePos, left);
        }
        for (Map.Entry<BlockVector3, BaseBlock> entry : copies.entrySet()) {
            finals.put(entry.getKey().add(offset), entry.getValue());
        }
        ResultBuilder builder = new ResultBuilder(prices, laborRates);
        com.sk89q.worldedit.extension.platform.Actor moveActor = actor;
        org.bukkit.entity.Player movePlayer = moveActor instanceof com.sk89q.worldedit.bukkit.BukkitPlayer bukkitPlayer
                ? bukkitPlayer.getPlayer()
                : null;
        // 被搬走的方块按种类记账：目标格原本就是同种方块（或受保护）时差异里看不到「放置」，
        // 只有拿这份清单去对冲，才不会把搬运当成拆除卖给市场。
        Map<String, Long> relocated = new HashMap<>();
        for (BaseBlock moved : copies.values()) {
            String id = itemIdFromBaseBlock(moved);
            if (!"air".equals(id)) {
                relocated.merge(id, 1L, Long::sum);
            }
        }
        builder.relocated(relocated);
        for (Map.Entry<BlockVector3, BaseBlock> entry : finals.entrySet()) {
            BlockVector3 pos = entry.getKey();
            if (BlockProtection.isProtectedWorldBlock(world, pos)) {
                builder.protectedBlocks++;
                continue;
            }
            if (!ResidenceProtection.canChange(movePlayer, world, pos, entry.getValue())) {
                builder.residenceDeniedBlocks++;
                continue;
            }
            BaseBlock before = original.get(pos);
            if (before == null) {
                before = BukkitSnapshotExtent.readBlock(world, pos).toBaseBlock();
            }
            builder.addChange(before, entry.getValue());
        }
        return builder.build();
    }

    /** FAWE 选区迭代器里的点会原地改坐标，不能直接当 Map 键或对其调用会 mutate 的 add()。 */
    static BlockVector3 freeze(BlockVector3 pos) {
        if (pos == null) {
            return BlockVector3.at(0, 0, 0);
        }
        return BlockVector3.at(pos.x(), pos.y(), pos.z());
    }

    static boolean isAirBlock(BaseBlock block) {
        if (block == null) {
            return true;
        }
        var type = block.getBlockType();
        return type == null || type.getMaterial().isAir();
    }

    public static String itemIdFromBaseBlock(BaseBlock block) {
        if (isAirBlock(block)) {
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
        private final double salvageRate;
        private final boolean netMoves;
        private double salvage;
        private double material;
        private double labor;
        private long affectedBlocks;
        long protectedBlocks;
        long residenceDeniedBlocks;
        private final Map<String, Long> removedCounts = new HashMap<>();
        private final Map<String, Long> placedCounts = new HashMap<>();
        /** //move 真正搬走的方块，按种类计数；目标格没变化时差异里没有对应的放置 */
        private Map<String, Long> relocatedCounts = Map.of();

        ResultBuilder(PriceCatalog prices, LaborRates laborRates) {
            this.prices = prices;
            this.laborRates = laborRates != null ? laborRates : LaborRates.defaults();
            McwwsWeSurvivalPlugin plugin = McwwsWeSurvivalPlugin.getInstance();
            this.salvageRate = plugin == null ? 0.8D : plugin.salvageRate();
            this.netMoves = plugin == null || plugin.netMoves();
        }

        void relocated(Map<String, Long> counts) {
            this.relocatedCounts = counts == null ? Map.of() : counts;
        }

        void addChange(BaseBlock existing, BaseBlock target) {
            if (existing == null || target == null) {
                return;
            }
            if (isAirBlock(existing) && isAirBlock(target)) {
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
                // 拆下来的方块进市场（MarketBridge 记 sell 侧库存），这里按卖价折现给玩家；
                // 劳务仍按拆除格数计费（与市价无关）
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

        Result build() {
            Map<String, Long> netRemoved = new HashMap<>(removedCounts);
            Map<String, Long> netPlaced = new HashMap<>(placedCounts);
            double netMaterial = material;
            double netSalvage = salvage;
            long moved = 0L;
            if (netMoves) {
                for (Map.Entry<String, Long> entry : removedCounts.entrySet()) {
                    String id = entry.getKey();
                    long placedSame = placedCounts.getOrDefault(id, 0L);
                    // 目标格原本就是同种方块时，差异里没有「放置」，得按实际搬走的数量对冲
                    long candidate = Math.max(placedSame, relocatedCounts.getOrDefault(id, 0L));
                    if (candidate <= 0L) {
                        continue;
                    }
                    // 同种方块「这里拆掉、那里放下」就是搬运：材料没消耗、市场也没进出，只该收劳务费
                    long count = Math.min(entry.getValue(), candidate);
                    moved += count;
                    // 材料只在真的记到放置时才会累加，扣减不能超过它，否则会连别的方块的材料费一起抹掉
                    netMaterial -= prices.getBuyPrice(id) * Math.min(count, placedSame);
                    netSalvage -= prices.getSellPrice(id) * salvageRate * count;
                    subtract(netRemoved, id, count);
                    subtract(netPlaced, id, count);
                }
            }
            // 物价表可能在预估途中重载，减出来的极小负值直接抹平
            salvage = Math.max(round(netSalvage), 0D);
            material = Math.max(round(netMaterial), 0D);
            labor = round(labor);
            return new Result(
                    salvage,
                    material,
                    labor,
                    affectedBlocks,
                    protectedBlocks,
                    residenceDeniedBlocks,
                    moved,
                    Map.copyOf(removedCounts),
                    Map.copyOf(placedCounts),
                    Map.copyOf(netRemoved),
                    Map.copyOf(netPlaced)
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
