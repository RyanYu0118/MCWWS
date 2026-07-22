/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 */
package ch.njol.skript.paperlib.features.blockstatesnapshot;

import ch.njol.skript.paperlib.features.blockstatesnapshot.BlockStateSnapshot;
import ch.njol.skript.paperlib.features.blockstatesnapshot.BlockStateSnapshotResult;
import org.bukkit.block.Block;

public class BlockStateSnapshotBeforeSnapshots
implements BlockStateSnapshot {
    @Override
    public BlockStateSnapshotResult getBlockState(Block block, boolean useSnapshot) {
        return new BlockStateSnapshotResult(false, block.getState());
    }
}

