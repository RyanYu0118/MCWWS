/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.Enderman
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Enderman Carrying BlockData")
@Description(value={"The block data an enderman is carrying.", "Custom attributes such as NBT or names do not transfer over.", "Blocks, blockdatas and items are acceptable objects to change the carrying block."})
@Example.Examples(value={@Example(value="broadcast the carrying blockdata of last spawned enderman"), @Example(value="set the carried block of last spawned enderman to an oak log"), @Example(value="set the carrying block data of {_enderman} to oak stairs[facing=north]"), @Example(value="set the carried blockdata of {_enderman} to {_item}"), @Example(value="clear the carried blockdata of {_enderman}")})
@Since(value={"2.11"})
public class ExprCarryingBlockData
extends SimplePropertyExpression<LivingEntity, BlockData> {
    @Override
    @Nullable
    public BlockData convert(LivingEntity entity) {
        if (entity instanceof Enderman) {
            Enderman enderman = (Enderman)entity;
            return enderman.getCarriedBlock();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) {
            return CollectionUtils.array(Block.class, BlockData.class, ItemType.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        BlockData data = null;
        if (delta != null) {
            Object object = delta[0];
            if (object instanceof BlockData) {
                BlockData blockData;
                data = blockData = (BlockData)object;
            } else {
                object = delta[0];
                if (object instanceof Block) {
                    Block block = (Block)object;
                    data = block.getBlockData();
                } else {
                    Material stackMaterial;
                    ItemStack itemStack = ItemUtils.asItemStack(delta[0]);
                    if (itemStack != null && (stackMaterial = itemStack.getType()).isBlock()) {
                        data = stackMaterial.createBlockData();
                    }
                }
            }
        }
        for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(event)) {
            if (!(entity instanceof Enderman)) continue;
            Enderman enderman = (Enderman)entity;
            enderman.setCarriedBlock(data);
        }
    }

    @Override
    public Class<? extends BlockData> getReturnType() {
        return BlockData.class;
    }

    @Override
    protected String getPropertyName() {
        return "carrying block data";
    }

    static {
        ExprCarryingBlockData.register(ExprCarryingBlockData.class, BlockData.class, "carr(ied|ying) block[[ ]data]", "livingentities");
    }
}

