/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.Directional
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Facing")
@Description(value={"The facing of an entity or block, i.e. exactly north, south, east, west, up or down (unlike <a href='#ExprDirection'>direction</a> which is the exact direction, e.g. '0.5 south and 0.7 east')"})
@Example(value="# makes a bridge\nloop blocks from the block below the player in the horizontal facing of the player:\n\tset loop-block to cobblestone\n")
@Since(value={"1.4"})
public class ExprFacing
extends SimplePropertyExpression<Object, Direction> {
    private boolean horizontal;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.horizontal = parseResult.mark == 1;
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Direction convert(Object o) {
        if (o instanceof Block) {
            BlockData data = ((Block)o).getBlockData();
            if (data instanceof Directional) {
                return new Direction(((Directional)data).getFacing(), 1.0);
            }
            return null;
        }
        if (o instanceof LivingEntity) {
            return new Direction(Direction.getFacing(((LivingEntity)o).getLocation(), this.horizontal), 1.0);
        }
        assert (false);
        return null;
    }

    @Override
    protected String getPropertyName() {
        return (this.horizontal ? "horizontal " : "") + "facing";
    }

    @Override
    public Class<Direction> getReturnType() {
        return Direction.class;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (!this.getExpr().canReturn(Block.class)) {
            return null;
        }
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(Direction.class);
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
        assert (mode == Changer.ChangeMode.SET);
        assert (delta != null);
        Block b = (Block)this.getExpr().getSingle(e);
        if (b == null) {
            return;
        }
        BlockData data = b.getBlockData();
        if (data instanceof Directional) {
            ((Directional)data).setFacing(ExprFacing.toBlockFace(((Direction)delta[0]).getDirection(b)));
            b.setBlockData(data, false);
        }
    }

    private static BlockFace toBlockFace(Vector dir) {
        BlockFace r = null;
        double d = Double.MAX_VALUE;
        for (BlockFace f : BlockFace.values()) {
            double a = Math.pow((double)f.getModX() - dir.getX(), 2.0) + Math.pow((double)f.getModY() - dir.getY(), 2.0) + Math.pow((double)f.getModZ() - dir.getZ(), 2.0);
            if (!(a < d)) continue;
            d = a;
            r = f;
        }
        assert (r != null);
        return r;
    }

    static {
        ExprFacing.register(ExprFacing.class, Direction.class, "(1\u00a6horizontal|) facing", "livingentities/blocks");
    }
}

