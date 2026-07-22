/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Direction")
@Description(value={"A helper expression for the <a href='#direction'>direction type</a>."})
@Example.Examples(value={@Example(value="thrust the player upwards"), @Example(value="set the block behind the player to water"), @Example(value="loop blocks above the player:\n\tset {_rand} to a random integer between 1 and 10\n\tset the block {_rand} meters south east of the loop-block to stone\n"), @Example(value="block in horizontal facing of the clicked entity from the player is air"), @Example(value="spawn a creeper 1.5 meters horizontally behind the player"), @Example(value="spawn a TNT 5 meters above and 2 meters horizontally behind the player"), @Example(value="thrust the last spawned TNT in the horizontal direction of the player with speed 0.2"), @Example(value="push the player upwards and horizontally forward at speed 0.5"), @Example(value="push the clicked entity in in the direction of the player at speed -0.5"), @Example(value="open the inventory of the block 2 blocks below the player to the player"), @Example(value="teleport the clicked entity behind the player"), @Example(value="grow a regular tree 2 meters horizontally behind the player")})
@Since(value={"1.0 (basic), 2.0 (extended)"})
public class ExprDirection
extends SimpleExpression<Direction> {
    private static final BlockFace[] byMark = new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST};
    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int NORTH = 2;
    private static final int SOUTH = 3;
    private static final int EAST = 4;
    private static final int WEST = 5;
    private static final int NORTH_EAST = 6;
    private static final int NORTH_WEST = 7;
    private static final int SOUTH_EAST = 8;
    private static final int SOUTH_WEST = 9;
    @Nullable
    Expression<Number> amount;
    @Nullable
    private Vector direction;
    @Nullable
    private ExprDirection next;
    @Nullable
    private Expression<?> relativeTo;
    boolean horizontal;
    boolean facing;
    private double yaw;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.amount = exprs[0];
        switch (matchedPattern) {
            case 0: {
                this.direction = new Vector(byMark[parseResult.mark].getModX(), byMark[parseResult.mark].getModY(), byMark[parseResult.mark].getModZ());
                if (exprs[1] == null) break;
                if (!(exprs[1] instanceof ExprDirection) || ((ExprDirection)exprs[1]).direction == null) {
                    return false;
                }
                this.next = (ExprDirection)exprs[1];
                break;
            }
            case 1: 
            case 2: {
                this.relativeTo = exprs[1];
                this.horizontal = parseResult.mark % 2 != 0;
                this.facing = parseResult.mark >= 2;
                break;
            }
            case 3: 
            case 4: {
                this.yaw = 1.5707963267948966 * (double)parseResult.mark;
                this.horizontal = matchedPattern == 4;
            }
        }
        return true;
    }

    @Nullable
    protected Direction[] get(Event e) {
        Integer n;
        Number number = n = this.amount != null ? (Number)this.amount.getSingle(e) : (Number)1;
        if (n == null) {
            return new Direction[0];
        }
        double ln = ((Number)n).doubleValue();
        if (this.direction != null) {
            Vector v = this.direction.clone().multiply(ln);
            ExprDirection d = this.next;
            while (d != null) {
                Integer n2;
                Number number2 = n2 = d.amount != null ? (Number)d.amount.getSingle(e) : (Number)1;
                if (n2 == null) {
                    return new Direction[0];
                }
                assert (d.direction != null);
                v.add(d.direction.clone().multiply(((Number)n2).doubleValue()));
                d = d.next;
            }
            assert (v != null);
            return new Direction[]{new Direction(v)};
        }
        if (this.relativeTo != null) {
            Object o = this.relativeTo.getSingle(e);
            if (o == null) {
                return new Direction[0];
            }
            if (o instanceof Block) {
                BlockFace f = Direction.getFacing((Block)o);
                if (f == BlockFace.SELF || this.horizontal && (f == BlockFace.UP || f == BlockFace.DOWN)) {
                    return new Direction[]{Direction.ZERO};
                }
                return new Direction[]{new Direction(f, ln)};
            }
            Location l = ((Entity)o).getLocation();
            if (!this.horizontal) {
                if (!this.facing) {
                    Vector v = l.getDirection().normalize().multiply(ln);
                    assert (v != null);
                    return new Direction[]{new Direction(v)};
                }
                double pitch = Direction.pitchToRadians(l.getPitch());
                assert (pitch >= -1.5707963267948966 && pitch <= 1.5707963267948966);
                if (pitch > 0.7853981633974483) {
                    return new Direction[]{new Direction(new double[]{0.0, ln, 0.0})};
                }
                if (pitch < -0.7853981633974483) {
                    return new Direction[]{new Direction(new double[]{0.0, -ln, 0.0})};
                }
            }
            double yaw = Direction.yawToRadians(l.getYaw());
            if (this.horizontal && !this.facing) {
                return new Direction[]{new Direction(new double[]{Math.cos(yaw) * ln, 0.0, Math.sin(yaw) * ln})};
            }
            if ((yaw = Math2.mod(yaw, Math.PI * 2)) >= 0.7853981633974483 && yaw < 2.356194490192345) {
                return new Direction[]{new Direction(new double[]{0.0, 0.0, ln})};
            }
            if (yaw >= 2.356194490192345 && yaw < 3.9269908169872414) {
                return new Direction[]{new Direction(new double[]{-ln, 0.0, 0.0})};
            }
            if (yaw >= 3.9269908169872414 && yaw < 5.497787143782138) {
                return new Direction[]{new Direction(new double[]{0.0, 0.0, -ln})};
            }
            assert (yaw >= 0.0 && yaw < 0.7853981633974483 || yaw >= 5.497787143782138 && yaw < Math.PI * 2);
            return new Direction[]{new Direction(new double[]{ln, 0.0, 0.0})};
        }
        return new Direction[]{new Direction(this.horizontal ? 61863.0 : 0.0, this.yaw, ln)};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Direction> getReturnType() {
        return Direction.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        Expression<?> relativeTo = this.relativeTo;
        return (String)(this.amount != null ? this.amount.toString(e, debug) + " meter(s) " : "") + (String)(this.direction != null ? Direction.toString(this.direction) : (relativeTo != null ? " in " + (this.horizontal ? "horizontal " : "") + (this.facing ? "facing" : "direction") + " of " + relativeTo.toString(e, debug) : (this.horizontal ? "horizontally " : "") + Direction.toString(0.0, this.yaw, 1.0)));
    }

    static {
        Skript.registerExpression(ExprDirection.class, Direction.class, ExpressionType.COMBINED, "[%-number% [(block|met(er|re))[s]] [to the]] (2\u00a6north[(-| |)(4\u00a6east|5\u00a6west)][(ward(s|ly|)|er(n|ly|))] [of]|3\u00a6south[(-| |)(11\u00a6east|10\u00a6west)][(ward(s|ly|)|er(n|ly|))] [of]|(4\u00a6east|5\u00a6west)[(ward(s|ly|)|er(n|ly|))] [of]|0\u00a6above|0\u00a6over|(0\u00a6up|1\u00a6down)[ward(s|ly|)]|1\u00a6below|1\u00a6under[neath]|1\u00a6beneath) [%-direction%]", "[%-number% [(block|met(er|re))[s]]] in [the] (0\u00a6direction|1\u00a6horizontal direction|2\u00a6facing|3\u00a6horizontal facing) of %entity/block% (of|from|)", "[%-number% [(block|met(er|re))[s]]] in %entity/block%'[s] (0\u00a6direction|1\u00a6horizontal direction|2\u00a6facing|3\u00a6horizontal facing) (of|from|)", "[%-number% [(block|met(er|re))[s]]] (0\u00a6in[ ]front [of]|0\u00a6forward[s]|2\u00a6behind|2\u00a6backwards|[to the] (1\u00a6right|-1\u00a6left) [of])", "[%-number% [(block|met(er|re))[s]]] horizontal[ly] (0\u00a6in[ ]front [of]|0\u00a6forward[s]|2\u00a6behind|2\u00a6backwards|to the (1\u00a6right|-1\u00a6left) [of])");
    }
}

