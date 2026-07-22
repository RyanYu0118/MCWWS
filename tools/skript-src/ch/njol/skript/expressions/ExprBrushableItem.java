/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.BrushableBlock
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrushableBlock;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Buried Item")
@Description(value={"Represents the item that is uncovered when dusting.", "The only blocks that can currently be \"dusted\" are Suspicious Gravel and Suspicious Sand."})
@Example.Examples(value={@Example(value="send target block's brushable item"), @Example(value="set {_gravel}'s brushable item to emerald")})
@Since(value={"2.12"})
@RequiredPlugins(value={"Minecraft 1.20+"})
public class ExprBrushableItem
extends SimplePropertyExpression<Block, ItemStack> {
    private static final boolean SUPPORTS_DUSTING = Skript.classExists("org.bukkit.block.BrushableBlock");

    @Override
    @Nullable
    public ItemStack convert(Block block) {
        BlockState blockState = block.getState();
        if (blockState instanceof BrushableBlock) {
            BrushableBlock brushableBlock = (BrushableBlock)blockState;
            return brushableBlock.getItem();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) {
            return CollectionUtils.array(ItemStack.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) {
            ItemStack newItem = mode == Changer.ChangeMode.SET ? (ItemStack)delta[0] : null;
            for (Block block : (Block[])this.getExpr().getArray(event)) {
                BlockState state = block.getState();
                if (!(state instanceof BrushableBlock)) continue;
                BrushableBlock brushableBlock = (BrushableBlock)state;
                brushableBlock.setItem(newItem);
                state.update();
            }
        }
    }

    @Override
    public Class<? extends ItemStack> getReturnType() {
        return ItemStack.class;
    }

    @Override
    protected String getPropertyName() {
        return "brushable item";
    }

    static {
        if (SUPPORTS_DUSTING) {
            ExprBrushableItem.register(ExprBrushableItem.class, ItemStack.class, "(brushable|buried) item", "blocks");
        }
    }
}

