/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.BlockDisplay
 *  org.bukkit.entity.FallingBlock
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Block Data")
@Description(value={"Get the <a href='#blockdata'>block data</a> associated with a block.", "This data can also be used to set blocks."})
@Example.Examples(value={@Example(value="set {_data} to block data of target block"), @Example(value="set block at player to {_data}"), @Example(value="set block data of target block to oak_stairs[facing=south;waterlogged=true]")})
@Since(value={"2.5, 2.5.2 (set), 2.10 (block displays)"})
public class ExprBlockData
extends SimplePropertyExpression<Object, BlockData> {
    @Override
    @Nullable
    public BlockData convert(Object object) {
        if (object instanceof Block) {
            Block block = (Block)object;
            return block.getBlockData();
        }
        if (object instanceof BlockDisplay) {
            BlockDisplay blockDisplay = (BlockDisplay)object;
            return blockDisplay.getBlock();
        }
        if (object instanceof FallingBlock) {
            FallingBlock fallingBlock = (FallingBlock)object;
            return fallingBlock.getBlockData();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(BlockData.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        assert (delta != null);
        BlockData blockData = (BlockData)delta[0];
        for (Object object : this.getExpr().getArray(event)) {
            if (object instanceof Block) {
                Block block = (Block)object;
                block.setBlockData(blockData);
                continue;
            }
            if (object instanceof BlockDisplay) {
                BlockDisplay blockDisplay = (BlockDisplay)object;
                blockDisplay.setBlock(blockData);
                continue;
            }
            if (!(object instanceof FallingBlock)) continue;
            FallingBlock fallingBlock = (FallingBlock)object;
            fallingBlock.setBlockData(blockData);
        }
    }

    @Override
    public Class<? extends BlockData> getReturnType() {
        return BlockData.class;
    }

    @Override
    protected String getPropertyName() {
        return "block data";
    }

    static {
        ExprBlockData.register(ExprBlockData.class, BlockData.class, "block[ ]data", "blocks/displays/entities");
    }
}

