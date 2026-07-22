/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.FallingBlock
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil.block;

import ch.njol.skript.bukkitutil.block.BlockSetter;
import ch.njol.skript.bukkitutil.block.BlockValues;
import ch.njol.skript.bukkitutil.block.NewBlockCompat;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface BlockCompat {
    public static final BlockCompat INSTANCE = new NewBlockCompat();
    public static final BlockSetter SETTER = INSTANCE.getSetter();

    @Deprecated(since="2.8.4", forRemoval=true)
    @Nullable
    public BlockValues getBlockValues(BlockState var1);

    @Nullable
    default public BlockValues getBlockValues(Block block) {
        return this.getBlockValues(block.getBlockData());
    }

    @Nullable
    public BlockValues getBlockValues(Material var1);

    @Nullable
    public BlockValues getBlockValues(BlockData var1);

    @Nullable
    public BlockValues getBlockValues(ItemStack var1);

    @Deprecated(since="2.8.4", forRemoval=true)
    public BlockState fallingBlockToState(FallingBlock var1);

    @Nullable
    default public BlockValues getBlockValues(FallingBlock entity) {
        return this.getBlockValues(entity.getBlockData());
    }

    @Nullable
    public BlockValues createBlockValues(Material var1, Map<String, String> var2, @Nullable ItemStack var3, int var4);

    @Nullable
    default public BlockValues createBlockValues(Material type, Map<String, String> states) {
        return this.createBlockValues(type, states, null, 0);
    }

    public BlockSetter getSetter();

    public boolean isEmpty(Material var1);

    public boolean isLiquid(Material var1);
}

