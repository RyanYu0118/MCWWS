/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil.block;

import ch.njol.skript.bukkitutil.block.BlockValues;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface BlockSetter {
    public static final int ROTATE = 1;
    public static final int ROTATE_FORCE = 2;
    public static final int ROTATE_FIX_TYPE = 4;
    public static final int MULTIPART = 8;
    public static final int APPLY_PHYSICS = 16;

    public void setBlock(Block var1, Material var2, @Nullable BlockValues var3, int var4);

    public void sendBlockChange(Player var1, Location var2, Material var3, @Nullable BlockValues var4);
}

