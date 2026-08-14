package work.mcwws.worldedit;

import com.fastasyncworldedit.core.util.MaskTraverser;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.generator.FloraGenerator;
import com.sk89q.worldedit.function.GroundFunction;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.NoiseFilter2D;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.noise.RandomNoise;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.Regions;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class WeShapeEstimate {

    private static final int DEFAULT_VERTICAL_HEIGHT = 256;

    private WeShapeEstimate() {
    }

    static FeeEstimate.Result estimate(
            String command,
            WeArgTokens tokens,
            boolean setAirAlias,
            PriceCatalog prices,
            FeeEstimate.LaborRates laborRates,
            LocalSession session,
            Actor actor,
            Player player,
            World world,
            Region region,
            long maxScan
    ) throws InputParseException {
        return switch (command) {
            case "set" -> {
                String pattern = setAirAlias || tokens.size() < 1 ? "air" : tokens.joinFrom(0);
                requireRegion(region);
                checkVolume(FeeEstimate.regionVolume(region), maxScan);
                yield FeeEstimate.forSet(prices, laborRates, region, world, pattern, actor);
            }
            case "replace" -> {
                requireRegion(region);
                checkVolume(FeeEstimate.regionVolume(region), maxScan);
                if (tokens.size() < 1) {
                    throw new InputParseException("replace 需要方块参数");
                }
                String from = tokens.size() < 2 ? "" : tokens.get(0);
                String to = tokens.size() < 2 ? tokens.joinFrom(0) : tokens.joinFrom(1);
                yield FeeEstimate.forReplace(prices, laborRates, region, world, from, to, actor);
            }
            case "cut" -> {
                requireRegion(region);
                checkVolume(FeeEstimate.regionVolume(region), maxScan);
                String leave = tokens.size() < 1 ? "air" : tokens.joinFrom(0);
                if (tokens.maskInput != null && !tokens.maskInput.isBlank()) {
                    yield FeeEstimate.forReplace(prices, laborRates, region, world, tokens.maskInput, leave, actor);
                }
                yield FeeEstimate.forSet(prices, laborRates, region, world, leave, actor);
            }
            case "move" -> {
                requireRegion(region);
                checkVolume(scanVolume(command, tokens, region, world), maxScan);
                yield estimateMove(tokens, prices, laborRates, actor, player, world, region);
            }
            case "walls", "overlay", "lay", "faces", "hollow", "center", "line", "curve",
                 "fall", "naturalize", "forest", "flora" -> {
                requireRegion(region);
                checkVolume(scanVolume(command, tokens, region, world), maxScan);
                FaweRegionSync.flushBeforeEstimate(world, region);
                EstimateContext.setRegion(region);
                yield dryRegion(command, tokens, prices, laborRates, session, actor, player, world, region);
            }
            case "paste", "place" -> estimateClipboard(command, tokens, prices, laborRates, session, actor, player, world, maxScan);
            case "cyl", "hcyl", "sphere", "hsphere", "pyramid", "hpyramid", "cone" ->
                    estimateGeneration(command, tokens, prices, laborRates, session, actor, player, world, maxScan);
            case "removenear", "removeabove", "removebelow", "drain", "extinguish",
                 "snow", "thaw", "green", "fixlava", "fixwater" ->
                    estimateUtility(command, tokens, prices, laborRates, session, actor, player, world, maxScan);
            default -> throw new UnsupportedOperationException(command);
        };
    }

    private static FeeEstimate.Result estimateMove(
            WeArgTokens tokens,
            PriceCatalog prices,
            FeeEstimate.LaborRates laborRates,
            Actor actor,
            Player player,
            World world,
            Region region
    ) throws InputParseException {
        int idx = 0;
        int count = 1;
        if (tokens.size() > 0 && WeArgTokens.isInt(tokens.get(0))) {
            count = Integer.parseInt(tokens.get(0));
            idx = 1;
        }
        if (count < 1) {
            throw new InputParseException("移动格数必须 >= 1");
        }
        BlockVector3 dir = WeDirection.aim(actor, player);
        if (idx < tokens.size()) {
            try {
                dir = WeDirection.parse(tokens.get(idx), actor, player);
                idx++;
            } catch (InputParseException ignored) {
                // 下一参数是留下的方块
            }
        }
        String leave = idx < tokens.size() ? tokens.joinFrom(idx) : "air";
        boolean copyAir = !tokens.has('a');
        BlockVector3 offset = dir.multiply(count);
        return FeeEstimate.forMove(prices, laborRates, region, world, offset, leave, copyAir, tokens.maskInput, actor);
    }

    private static FeeEstimate.Result dryRegion(
            String command,
            WeArgTokens tokens,
            PriceCatalog prices,
            FeeEstimate.LaborRates laborRates,
            LocalSession session,
            Actor actor,
            Player player,
            World world,
            Region region
    ) throws InputParseException {
        Pattern pattern;
        FeeEstimate.ResultBuilder builder = new FeeEstimate.ResultBuilder(prices, laborRates);
        try {
            EstimateDrySession.run(world, builder, (edit, counter) -> {
                switch (command) {
                    case "walls" -> edit.makeWalls(region, requirePattern(tokens, actor, world));
                    case "faces" -> edit.makeFaces(region, requirePattern(tokens, actor, world));
                    case "overlay" -> edit.overlayCuboidBlocks(region, requirePattern(tokens, actor, world));
                    case "lay" -> {
                        Pattern layPattern = requirePattern(tokens, actor, world);
                        BlockVector3 min = region.getMinimumPoint();
                        BlockVector3 max = region.getMaximumPoint();
                        for (int x = min.x(); x <= max.x(); x++) {
                            for (int z = min.z(); z <= max.z(); z++) {
                                for (int y = max.y(); y >= min.y(); y--) {
                                    BlockVector3 at = BlockVector3.at(x, y, z);
                                    if (!region.contains(at)) {
                                        continue;
                                    }
                                    if (!edit.getBlock(at).getBlockType().getMaterial().isAir()) {
                                        edit.setBlock(at, layPattern.applyBlock(at));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    case "center" -> edit.center(region, requirePattern(tokens, actor, world));
                    case "hollow" -> {
                        int thickness = 0;
                        String patternInput = "air";
                        if (tokens.size() >= 1) {
                            if (WeArgTokens.isInt(tokens.get(0))) {
                                thickness = Integer.parseInt(tokens.get(0));
                                patternInput = tokens.size() >= 2 ? tokens.joinFrom(1) : "air";
                            } else {
                                patternInput = tokens.joinFrom(0);
                            }
                        }
                        if (thickness < 0) {
                            throw new InputParseException("空心厚度不能为负");
                        }
                        Pattern fill = parsePattern(patternInput, actor, world);
                        Mask hollowMask = tokens.maskInput != null
                                ? parseMask(tokens.maskInput, actor, world)
                                : new SolidBlockMask(edit);
                        new MaskTraverser(hollowMask).setNewExtent(edit);
                        edit.hollowOutRegion(region, thickness, fill, hollowMask);
                    }
                    case "line" -> {
                        if (!(region instanceof CuboidRegion cuboid)) {
                            throw new InputParseException("need-cuboid");
                        }
                        Pattern linePattern = requirePattern(tokens, actor, world);
                        double thickness = 0D;
                        if (tokens.size() >= 2 && WeArgTokens.isDouble(tokens.get(1))) {
                            thickness = Double.parseDouble(tokens.get(1));
                        }
                        edit.drawLine(linePattern, cuboid.getPos1(), cuboid.getPos2(), thickness, !tokens.has('h'));
                    }
                    case "curve" -> {
                        if (!(region instanceof ConvexPolyhedralRegion convex)) {
                            throw new InputParseException("need-convex");
                        }
                        Pattern curvePattern = requirePattern(tokens, actor, world);
                        double thickness = 0D;
                        if (tokens.size() >= 2 && WeArgTokens.isDouble(tokens.get(1))) {
                            thickness = Double.parseDouble(tokens.get(1));
                        }
                        List<BlockVector3> vertices = new ArrayList<>(convex.getVertices());
                        edit.drawSpline(curvePattern, vertices, 0, 0, 0, 10, thickness, !tokens.has('h'));
                    }
                    case "fall" -> {
                        String replaceInput = tokens.get(0, "air");
                        Pattern replacePattern = parsePattern(replaceInput, actor, world);
                        edit.fall(region, !tokens.has('m'), replacePattern.applyBlock(region.getMinimumPoint()));
                    }
                    case "naturalize" -> edit.naturalizeCuboidBlocks(region);
                    case "forest" -> {
                        String typeName = "tree";
                        double density = 5D;
                        if (tokens.size() >= 1) {
                            if (tokens.size() == 1 && WeArgTokens.isDouble(tokens.get(0))) {
                                density = Double.parseDouble(tokens.get(0));
                            } else {
                                typeName = tokens.get(0);
                                if (tokens.size() >= 2) {
                                    density = Double.parseDouble(tokens.get(1));
                                }
                            }
                        }
                        if (density < 0D || density > 100D) {
                            throw new InputParseException("树林密度须在 0 到 100 之间");
                        }
                        TreeGenerator.TreeType type = TreeGenerator.TreeType.lookup(typeName);
                        if (type == null) {
                            throw new InputParseException("未知树木类型: " + typeName);
                        }
                        edit.makeForest(region, density / 100D, type);
                    }
                    case "flora" -> {
                        double density = tokens.size() >= 1 ? Double.parseDouble(tokens.get(0)) : 5D;
                        if (density < 0D || density > 100D) {
                            throw new InputParseException("植被密度须在 0 到 100 之间");
                        }
                        FloraGenerator generator = new FloraGenerator(edit);
                        GroundFunction ground = new GroundFunction(new ExistingBlockMask(edit), generator);
                        LayerVisitor visitor = new LayerVisitor(
                                Regions.asFlatRegion(region),
                                region.getMinimumPoint().y(),
                                region.getMaximumPoint().y(),
                                ground
                        );
                        visitor.setMask(new NoiseFilter2D(new RandomNoise(), density / 100D));
                        Operations.completeLegacy(visitor);
                    }
                    default -> throw new UnsupportedOperationException(command);
                }
            });
        } catch (RuntimeException ex) {
            throw unwrap(ex);
        }
        return builder.build();
    }

    private static FeeEstimate.Result estimateClipboard(
            String command,
            WeArgTokens tokens,
            PriceCatalog prices,
            FeeEstimate.LaborRates laborRates,
            LocalSession session,
            Actor actor,
            Player player,
            World world,
            long maxScan
    ) throws InputParseException {
        if (tokens.has('n')) {
            return FeeEstimate.empty();
        }
        ClipboardHolder holder;
        try {
            holder = session.getClipboard();
        } catch (EmptyClipboardException ex) {
            throw new InputParseException("empty-clipboard");
        }
        Clipboard clipboard = holder.getClipboard();
        long volume = clipboard.getVolume();
        checkVolume(volume, maxScan);
        BlockVector3 to = tokens.has('o') ? clipboard.getOrigin() : placement(session, actor, player);
        Transform transform = holder.getTransform();
        Region clipRegion = clipboard.getRegion();
        BlockVector3 min = clipRegion.getMinimumPoint();
        BlockVector3 max = clipRegion.getMaximumPoint();
        BlockVector3 origin = clipboard.getOrigin();
        BlockVector3 destMin = to.add(transform.apply(min.subtract(origin).toVector3()).toBlockPoint());
        BlockVector3 destMax = to.add(transform.apply(max.subtract(origin).toVector3()).toBlockPoint());
        FaweRegionSync.flushBeforeEstimate(world, destMin, destMax);
        EstimateContext.setRegion(new CuboidRegion(destMin, destMax));
        FeeEstimate.ResultBuilder builder = new FeeEstimate.ResultBuilder(prices, laborRates);
        boolean ignoreAir = tokens.has('a');
        try {
            EstimateDrySession.run(world, builder, (edit, counter) -> {
                if ("place".equals(command) || transform.isIdentity()) {
                    clipboard.paste(counter, to, !ignoreAir);
                } else {
                    Operations.completeBlindly(
                            holder.createPaste(counter)
                                    .to(to)
                                    .ignoreAirBlocks(ignoreAir)
                                    .build()
                    );
                }
            });
        } catch (RuntimeException ex) {
            throw unwrap(ex);
        }
        return builder.build();
    }

    private static FeeEstimate.Result estimateGeneration(
            String command,
            WeArgTokens tokens,
            PriceCatalog prices,
            FeeEstimate.LaborRates laborRates,
            LocalSession session,
            Actor actor,
            Player player,
            World world,
            long maxScan
    ) throws InputParseException {
        if (tokens.size() < 2) {
            throw new InputParseException("缺少方块或半径参数");
        }
        Pattern pattern = parsePattern(tokens.get(0), actor, world);
        BlockVector3 pos = placement(session, actor, player);
        FeeEstimate.ResultBuilder builder = new FeeEstimate.ResultBuilder(prices, laborRates);
        try {
            EstimateDrySession.run(world, builder, (edit, counter) -> {
                switch (command) {
                    case "cyl", "hcyl" -> {
                        double[] radii = WeArgTokens.parseRadii(tokens.get(1), 2);
                        double radiusX = Math.max(1D, radii[0]);
                        double radiusZ = radii.length > 1 ? Math.max(1D, radii[1]) : radiusX;
                        int height = tokens.size() >= 3 ? Integer.parseInt(tokens.get(2)) : 1;
                        double thickness = tokens.size() >= 4 ? Double.parseDouble(tokens.get(3)) : 0D;
                        boolean filled = "cyl".equals(command) && !tokens.has('h');
                        checkVolume(cylVolume(radiusX, radiusZ, height), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, (int) Math.ceil(Math.max(radiusX, radiusZ)) + Math.abs(height));
                        if ("hcyl".equals(command) || thickness > 0D) {
                            edit.makeCylinder(pos, pattern, radiusX, radiusZ, height, thickness, false);
                        } else {
                            edit.makeCylinder(pos, pattern, radiusX, radiusZ, height, filled);
                        }
                    }
                    case "sphere", "hsphere" -> {
                        double[] radii = WeArgTokens.parseRadii(tokens.get(1), 3);
                        double radiusX = Math.max(0D, radii[0]);
                        double radiusY = radii.length > 1 ? Math.max(0D, radii[1]) : radiusX;
                        double radiusZ = radii.length > 2 ? Math.max(0D, radii[2]) : radiusX;
                        boolean raised = tokens.has('r');
                        boolean filled = "sphere".equals(command) && !tokens.has('h');
                        checkVolume(sphereVolume(radiusX, radiusY, radiusZ), maxScan);
                        BlockVector3 center = raised ? pos.add(0, (int) radiusY, 0) : pos;
                        FaweRegionSync.flushBeforeEstimate(world, center, (int) Math.ceil(Math.max(radiusX, Math.max(radiusY, radiusZ))));
                        edit.makeSphere(center, pattern, radiusX, radiusY, radiusZ, filled);
                    }
                    case "pyramid", "hpyramid" -> {
                        int size = Integer.parseInt(tokens.get(1));
                        boolean filled = "pyramid".equals(command) && !tokens.has('h');
                        checkVolume((long) (2L * size + 1L) * (size + 1L) * (2L * size + 1L), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, size);
                        edit.makePyramid(pos, pattern, size, filled);
                    }
                    case "cone" -> {
                        double[] radii = WeArgTokens.parseRadii(tokens.get(1), 2);
                        double radiusX = Math.max(1D, radii[0]);
                        double radiusZ = radii.length > 1 ? Math.max(1D, radii[1]) : radiusX;
                        int height = tokens.size() >= 3 ? Integer.parseInt(tokens.get(2)) : 1;
                        double thickness = tokens.size() >= 4 ? Double.parseDouble(tokens.get(3)) : 1D;
                        boolean filled = !tokens.has('h');
                        checkVolume(cylVolume(radiusX, radiusZ, height), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, (int) Math.ceil(Math.max(radiusX, radiusZ)) + Math.abs(height));
                        edit.makeCone(pos, pattern, radiusX, radiusZ, height, filled, thickness);
                    }
                    default -> throw new UnsupportedOperationException(command);
                }
            });
        } catch (RuntimeException ex) {
            throw unwrap(ex);
        }
        return builder.build();
    }

    private static FeeEstimate.Result estimateUtility(
            String command,
            WeArgTokens tokens,
            PriceCatalog prices,
            FeeEstimate.LaborRates laborRates,
            LocalSession session,
            Actor actor,
            Player player,
            World world,
            long maxScan
    ) throws InputParseException {
        BlockVector3 pos = placement(session, actor, player);
        int worldHeight = world.getMaxY() - world.getMinY() + 1;
        FeeEstimate.ResultBuilder builder = new FeeEstimate.ResultBuilder(prices, laborRates);
        try {
            EstimateDrySession.run(world, builder, (edit, counter) -> {
                switch (command) {
                    case "removenear" -> {
                        if (tokens.size() < 1) {
                            throw new InputParseException("removenear 需要方块/掩码");
                        }
                        int radius = tokens.size() >= 2 ? Math.max(1, Integer.parseInt(tokens.get(1))) : 50;
                        checkVolume(FeeEstimate.replaceNearScanVolume(radius), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, radius);
                        Mask mask = parseMask(tokens.get(0), actor, world);
                        new MaskTraverser(mask).setNewExtent(edit);
                        edit.removeNear(pos, mask, radius);
                    }
                    case "removeabove" -> {
                        int size = tokens.size() >= 1 ? Math.max(1, Integer.parseInt(tokens.get(0))) : 1;
                        int height = tokens.size() >= 2 ? Math.max(1, Integer.parseInt(tokens.get(1))) : worldHeight;
                        checkVolume((long) (2L * size) * height * (2L * size), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, Math.max(size, height));
                        edit.removeAbove(pos, size, height);
                    }
                    case "removebelow" -> {
                        int size = tokens.size() >= 1 ? Math.max(1, Integer.parseInt(tokens.get(0))) : 1;
                        int height = tokens.size() >= 2 ? Math.max(1, Integer.parseInt(tokens.get(1))) : worldHeight;
                        checkVolume((long) (2L * size) * height * (2L * size), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, Math.max(size, height));
                        edit.removeBelow(pos, size, height);
                    }
                    case "drain" -> {
                        if (tokens.size() < 1 || !WeArgTokens.isDouble(tokens.get(0))) {
                            throw new InputParseException("drain 需要半径");
                        }
                        double radius = Math.max(0D, Double.parseDouble(tokens.get(0)));
                        int ir = (int) Math.ceil(radius);
                        checkVolume(FeeEstimate.replaceNearScanVolume(ir), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, ir);
                        edit.drainArea(pos, radius, tokens.has('w'), tokens.has('p'));
                    }
                    case "extinguish" -> {
                        int radius = tokens.size() >= 1 ? Math.max(1, Integer.parseInt(tokens.get(0))) : 40;
                        checkVolume(FeeEstimate.replaceNearScanVolume(radius), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, radius);
                        Mask fire = new BlockTypeMask(edit, BlockTypes.FIRE);
                        edit.removeNear(pos, fire, radius);
                    }
                    case "snow" -> {
                        double size = tokens.size() >= 1 ? Math.max(1D, Double.parseDouble(tokens.get(0))) : 10D;
                        int height = tokens.size() >= 2 ? Math.max(1, Integer.parseInt(tokens.get(1))) : DEFAULT_VERTICAL_HEIGHT;
                        int ir = (int) Math.ceil(size);
                        checkVolume((long) (2L * ir + 1L) * (2L * height + 1L) * (2L * ir + 1L), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, Math.max(ir, height));
                        CylinderRegion snowRegion = new CylinderRegion(
                                pos,
                                Vector2.at(size, size),
                                pos.y() - height,
                                pos.y() + height
                        );
                        edit.simulateSnow(snowRegion, tokens.has('s'));
                    }
                    case "thaw" -> {
                        double size = tokens.size() >= 1 ? Math.max(1D, Double.parseDouble(tokens.get(0))) : 10D;
                        int height = tokens.size() >= 2 ? Math.max(1, Integer.parseInt(tokens.get(1))) : DEFAULT_VERTICAL_HEIGHT;
                        int ir = (int) Math.ceil(size);
                        checkVolume((long) (2L * ir + 1L) * (2L * height + 1L) * (2L * ir + 1L), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, Math.max(ir, height));
                        edit.thaw(pos, size, height);
                    }
                    case "green" -> {
                        double size = tokens.size() >= 1 ? Math.max(1D, Double.parseDouble(tokens.get(0))) : 10D;
                        int height = tokens.size() >= 2 ? Math.max(1, Integer.parseInt(tokens.get(1))) : DEFAULT_VERTICAL_HEIGHT;
                        int ir = (int) Math.ceil(size);
                        checkVolume((long) (2L * ir + 1L) * (2L * height + 1L) * (2L * ir + 1L), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, Math.max(ir, height));
                        edit.green(pos, size, height, !tokens.has('f'));
                    }
                    case "fixlava" -> {
                        if (tokens.size() < 1) {
                            throw new InputParseException("fixlava 需要半径");
                        }
                        double radius = Math.max(0D, Double.parseDouble(tokens.get(0)));
                        int ir = (int) Math.ceil(radius);
                        checkVolume(FeeEstimate.replaceNearScanVolume(ir), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, ir);
                        edit.fixLiquid(pos, radius, BlockTypes.LAVA);
                    }
                    case "fixwater" -> {
                        if (tokens.size() < 1) {
                            throw new InputParseException("fixwater 需要半径");
                        }
                        double radius = Math.max(0D, Double.parseDouble(tokens.get(0)));
                        int ir = (int) Math.ceil(radius);
                        checkVolume(FeeEstimate.replaceNearScanVolume(ir), maxScan);
                        FaweRegionSync.flushBeforeEstimate(world, pos, ir);
                        edit.fixLiquid(pos, radius, BlockTypes.WATER);
                    }
                    default -> throw new UnsupportedOperationException(command);
                }
            });
        } catch (RuntimeException ex) {
            throw unwrap(ex);
        }
        return builder.build();
    }

    static World resolveWorld(LocalSession session, Player player) {
        World world = session.getSelectionWorld();
        if (world == null) {
            world = BukkitAdapter.adapt(player.getWorld());
        }
        return world;
    }

    static BlockVector3 placement(LocalSession session, Actor actor, Player player) {
        try {
            return session.getPlacementPosition(actor);
        } catch (Exception ex) {
            Location loc = player.getLocation();
            return BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
    }

    static boolean needsSelection(String command) {
        return switch (command) {
            case "paste", "place", "cyl", "hcyl", "sphere", "hsphere", "pyramid", "hpyramid", "cone",
                 "replacenear", "removenear", "removeabove", "removebelow", "drain", "extinguish",
                 "snow", "thaw", "green", "fixlava", "fixwater" -> false;
            default -> true;
        };
    }

    private static Pattern requirePattern(WeArgTokens tokens, Actor actor, World world) throws InputParseException {
        if (tokens.size() < 1) {
            throw new InputParseException("缺少方块参数");
        }
        return parsePattern(tokens.get(0), actor, world);
    }

    private static Pattern parsePattern(String input, Actor actor, World world) throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(world);
        return WorldEdit.getInstance().getPatternFactory().parseFromInput(input, context);
    }

    private static Mask parseMask(String input, Actor actor, World world) throws InputParseException {
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(world);
        return WorldEdit.getInstance().getMaskFactory().parseFromInput(input, context);
    }

    private static void requireRegion(Region region) throws InputParseException {
        if (region == null) {
            throw new InputParseException("no-selection");
        }
    }

    private static void checkVolume(long volume, long maxScan) throws InputParseException {
        if (volume > maxScan) {
            throw new InputParseException("scan-too-large");
        }
    }

    private static long scanVolume(String command, WeArgTokens tokens, Region region, World world) {
        long base = FeeEstimate.regionVolume(region);
        if ("move".equals(command)) {
            return base * 2L;
        }
        return base;
    }

    private static long cylVolume(double radiusX, double radiusZ, int height) {
        long dx = (long) Math.ceil(radiusX) * 2L + 1L;
        long dz = (long) Math.ceil(radiusZ) * 2L + 1L;
        return dx * dz * Math.max(1, Math.abs(height));
    }

    private static long sphereVolume(double radiusX, double radiusY, double radiusZ) {
        long dx = (long) Math.ceil(radiusX) * 2L + 1L;
        long dy = (long) Math.ceil(radiusY) * 2L + 1L;
        long dz = (long) Math.ceil(radiusZ) * 2L + 1L;
        return dx * dy * dz;
    }

    private static InputParseException unwrap(RuntimeException ex) throws InputParseException {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof InputParseException parse) {
                throw parse;
            }
            if (current instanceof UnsupportedOperationException unsupported) {
                throw unsupported;
            }
            current = current.getCause();
        }
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        throw new InputParseException(message);
    }
}
