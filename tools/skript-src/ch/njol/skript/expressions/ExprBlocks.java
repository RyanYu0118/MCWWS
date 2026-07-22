/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.bukkit.Chunk
 *  org.bukkit.Location
 *  org.bukkit.block.Block
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprDirection;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.AABB;
import ch.njol.skript.util.BlockLineIterator;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;
import com.google.common.collect.Lists;
import java.util.Iterator;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Blocks")
@Description(value={"Blocks relative to other blocks or between other blocks.", "Can be used to get blocks relative to other blocks or for looping.", "Blocks from/to and between will return a straight line whereas blocks within will return a cuboid."})
@Example.Examples(value={@Example(value="loop blocks above the player:"), @Example(value="loop blocks between the block below the player and the targeted block:"), @Example(value="set the blocks below the player, the victim and the targeted block to air"), @Example(value="set all blocks within {loc1} and {loc2} to stone"), @Example(value="set all blocks within chunk at player to air")})
@Since(value={"1.0, 2.5.1 (within/cuboid/chunk)"})
public class ExprBlocks
extends SimpleExpression<Block> {
    private static final boolean SUPPORTS_WORLD_LOADED = Skript.methodExists(Location.class, "isWorldLoaded", new Class[0]);
    @Nullable
    private Expression<Direction> direction;
    @Nullable
    private Expression<Location> end;
    @Nullable
    private Expression<Chunk> chunk;
    private Expression<?> from;
    private int pattern;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.pattern = matchedPattern;
        switch (matchedPattern) {
            case 0: {
                this.direction = exprs[0];
                this.from = exprs[1];
                break;
            }
            case 1: {
                this.from = exprs[0];
                this.direction = exprs[1];
                break;
            }
            case 2: 
            case 3: 
            case 4: {
                this.from = exprs[0];
                this.end = exprs[1];
                break;
            }
            case 5: {
                this.chunk = exprs[0];
                break;
            }
            default: {
                assert (false) : matchedPattern;
                return false;
            }
        }
        return true;
    }

    @Nullable
    protected Block[] get(Event event) {
        if (this.direction != null && !this.from.isSingle()) {
            Direction direction = this.direction.getSingle(event);
            if (direction == null) {
                return new Block[0];
            }
            return (Block[])this.from.stream(event).filter(Location.class::isInstance).map(Location.class::cast).filter(location -> {
                if (SUPPORTS_WORLD_LOADED) {
                    return location.isWorldLoaded();
                }
                return location.getChunk().isLoaded();
            }).map(direction::getRelative).map(Location::getBlock).toArray(Block[]::new);
        }
        Iterator<Block> iterator = this.iterator(event);
        if (iterator == null) {
            return new Block[0];
        }
        return Lists.newArrayList(iterator).toArray(new Block[0]);
    }

    @Override
    @Nullable
    public Iterator<Block> iterator(Event event) {
        block15: {
            try {
                if (this.chunk != null) {
                    Chunk chunk = this.chunk.getSingle(event);
                    if (chunk != null) {
                        return new AABB(chunk).iterator();
                    }
                    break block15;
                }
                if (this.direction != null) {
                    Number number;
                    Expression<Number> numberExpression;
                    Vector vector;
                    if (!this.from.isSingle()) {
                        return new ArrayIterator<Block>(this.get(event));
                    }
                    Object object = this.from.getSingle(event);
                    if (object == null) {
                        return null;
                    }
                    Location location = object instanceof Location ? (Location)object : ((Block)object).getLocation().add(0.5, 0.5, 0.5);
                    Direction direction = this.direction.getSingle(event);
                    if (direction == null || location.getWorld() == null) {
                        return null;
                    }
                    Vector vector2 = vector = object != location ? direction.getDirection((Block)object) : direction.getDirection(location);
                    if (vector.getX() == 0.0 && vector.getY() == 0.0 && vector.getZ() == 0.0) {
                        return null;
                    }
                    int distance = SkriptConfig.maxTargetBlockDistance.value() - 1;
                    if (this.direction instanceof ExprDirection && (numberExpression = ((ExprDirection)this.direction).amount) != null && (number = numberExpression.getSingle(event)) != null) {
                        distance = number.intValue();
                    }
                    return new BlockLineIterator(location, vector, (double)distance);
                }
                Location loc = (Location)this.from.getSingle(event);
                if (loc == null) {
                    return null;
                }
                assert (this.end != null);
                Location loc2 = this.end.getSingle(event);
                if (loc2 == null || loc2.getWorld() != loc.getWorld()) {
                    return null;
                }
                if (this.pattern == 4) {
                    return new AABB(loc, loc2).iterator();
                }
                return new BlockLineIterator(loc.getBlock(), loc2.getBlock());
            }
            catch (IllegalStateException e) {
                if (e.getMessage().equals("Start block missed in BlockIterator")) {
                    return null;
                }
                throw e;
            }
        }
        return null;
    }

    @Override
    public Class<? extends Block> getReturnType() {
        return Block.class;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.chunk != null) {
            return "blocks within chunk " + this.chunk.toString(event, debug);
        }
        if (this.pattern == 4) {
            assert (this.end != null);
            return "blocks within " + this.from.toString(event, debug) + " and " + this.end.toString(event, debug);
        }
        if (this.end != null) {
            return "blocks from " + this.from.toString(event, debug) + " to " + this.end.toString(event, debug);
        }
        assert (this.direction != null);
        return "blocks " + this.direction.toString(event, debug) + " " + this.from.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprBlocks.class, Block.class, ExpressionType.COMBINED, "[(all [[of] the]|the)] blocks %direction% [%locations%]", "[(all [[of] the]|the)] blocks from %location% [on] %direction%", "[(all [[of] the]|the)] blocks from %location% to %location%", "[(all [[of] the]|the)] blocks between %location% and %location%", "[(all [[of] the]|the)] blocks within %location% and %location%", "[(all [[of] the]|the)] blocks (in|within) %chunk%");
    }
}

