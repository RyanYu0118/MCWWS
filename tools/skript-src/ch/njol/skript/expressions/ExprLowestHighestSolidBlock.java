/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.generator.WorldInfo
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.Nullable;

@Name(value="Lowest/Highest Solid Block")
@Description(value={"An expression to obtain the lowest or highest solid (impassable) block at a location.", "Note that the y-coordinate of the location is not taken into account for this expression."})
@Example.Examples(value={@Example(value="teleport the player to the block above the highest block at the player"), @Example(value="set the highest solid block at the player's location to the lowest solid block at the player's location")})
@Since(value={"2.2-dev34, 2.9.0 (lowest solid block, 'non-air' option removed, additional syntax option)"})
public class ExprLowestHighestSolidBlock
extends SimplePropertyExpression<Location, Block> {
    private static final boolean HAS_MIN_HEIGHT = Skript.classExists("org.bukkit.generator.WorldInfo") && Skript.methodExists(WorldInfo.class, "getMinHeight", new Class[0]);
    private static final boolean HAS_BLOCK_IS_SOLID = Skript.methodExists(Block.class, "isSolid", new Class[0]);
    private static final boolean RETURNS_FIRST_AIR = !Skript.isRunningMinecraft(1, 15);
    private boolean lowest;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.lowest = parseResult.hasTag("lowest");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Block convert(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        if (!this.lowest) {
            return ExprLowestHighestSolidBlock.getHighestBlockAt(world, location);
        }
        location = location.clone();
        location.setY(HAS_MIN_HEIGHT ? (double)world.getMinHeight() : 0.0);
        Block block = location.getBlock();
        int maxHeight = world.getMaxHeight();
        while (block.getY() < maxHeight && !ExprLowestHighestSolidBlock.isSolid(block)) {
            block = block.getRelative(BlockFace.UP);
        }
        return ExprLowestHighestSolidBlock.isSolid(block) ? block : ExprLowestHighestSolidBlock.getHighestBlockAt(world, block.getLocation());
    }

    private static Block getHighestBlockAt(World world, Location location) {
        Block block = world.getHighestBlockAt(location);
        if (RETURNS_FIRST_AIR && !ExprLowestHighestSolidBlock.isSolid(block = block.getRelative(BlockFace.DOWN))) {
            block.getRelative(BlockFace.UP);
        }
        return block;
    }

    private static boolean isSolid(Block block) {
        return HAS_BLOCK_IS_SOLID ? block.isSolid() : block.getType().isSolid();
    }

    @Override
    public Class<? extends Block> getReturnType() {
        return Block.class;
    }

    @Override
    protected String getPropertyName() {
        return (this.lowest ? "lowest" : "highest") + " solid block";
    }

    static {
        Skript.registerExpression(ExprLowestHighestSolidBlock.class, Block.class, ExpressionType.PROPERTY, "[the] (highest|:lowest) [solid] block (at|of) %locations%", "%locations%'[s] (highest|:lowest) [solid] block");
    }
}

