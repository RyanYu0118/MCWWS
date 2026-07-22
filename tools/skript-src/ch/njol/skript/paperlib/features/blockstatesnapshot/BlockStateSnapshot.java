/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 */
package ch.njol.skript.paperlib.features.blockstatesnapshot;

import ch.njol.skript.paperlib.features.blockstatesnapshot.BlockStateSnapshotResult;
import org.bukkit.block.Block;

public interface BlockStateSnapshot {
    public BlockStateSnapshotResult getBlockState(Block var1, boolean var2);
}

