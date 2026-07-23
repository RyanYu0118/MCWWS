package work.mcwws.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Locale;

public final class FeeEstimate {

    public record Result(double demolition, double material, double labor, long affectedBlocks, long protectedBlocks) {
        public double total() {
            return demolition + material + labor;
        }
    }

    private FeeEstimate() {
    }

    public static Result empty() {
        return new Result(0D, 0D, 0D, 0L, 0L);
    }

    public static Result forSet(PriceCatalog prices, Region region, World world, String patternInput, com.sk89q.worldedit.extension.platform.Actor actor) throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(world);
        Pattern pattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(patternInput, context);
        ResultBuilder builder = new ResultBuilder(prices);
        for (BlockVector3 pos : region) {
            if (BlockProtection.isProtectedWorldBlock(world, pos)) {
                builder.protectedBlocks++;
                continue;
            }
            BaseBlock existing = world.getBlock(pos).toBaseBlock();
            BaseBlock target = pattern.applyBlock(pos);
            builder.addChange(existing, target);
        }
        return builder.build();
    }

    public static Result forReplace(PriceCatalog prices, Region region, World world, String fromInput, String toInput, com.sk89q.worldedit.extension.platform.Actor actor) throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(world);
        Pattern fromPattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(fromInput, context);
        Pattern toPattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(toInput, context);
        ResultBuilder builder = new ResultBuilder(prices);
        for (BlockVector3 pos : region) {
            if (BlockProtection.isProtectedWorldBlock(world, pos)) {
                builder.protectedBlocks++;
                continue;
            }
            BaseBlock existing = world.getBlock(pos).toBaseBlock();
            if (!fromPattern.applyBlock(pos).equals(existing)) {
                continue;
            }
            BaseBlock target = toPattern.applyBlock(pos);
            builder.addChange(existing, target);
        }
        return builder.build();
    }

    public static Result forUniformPattern(PriceCatalog prices, Region region, World world, String patternInput, com.sk89q.worldedit.extension.platform.Actor actor) throws InputParseException {
        return forSet(prices, region, world, patternInput, actor);
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

    private static final class ResultBuilder {
        private final PriceCatalog prices;
        private double demolition;
        private double material;
        private double labor;
        private long affectedBlocks;
        private long protectedBlocks;

        private ResultBuilder(PriceCatalog prices) {
            this.prices = prices;
        }

        private void addChange(BaseBlock existing, BaseBlock target) {
            if (existing == null || target == null) {
                return;
            }
            if (existing.equals(target)) {
                return;
            }
            String oldId = itemIdFromBaseBlock(existing);
            String newId = itemIdFromBaseBlock(target);
            if (!"air".equals(oldId)) {
                demolition += prices.getBuyPrice(oldId);
            }
            if (!"air".equals(newId)) {
                double unit = prices.getBuyPrice(newId);
                material += unit;
                labor += unit;
            }
            affectedBlocks++;
        }

        private Result build() {
            demolition = round(demolition);
            material = round(material);
            labor = round(labor);
            return new Result(demolition, material, labor, affectedBlocks, protectedBlocks);
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
