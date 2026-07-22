/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.BlockState
 */
package ch.njol.skript.paperlib.features.blockstatesnapshot;

import org.bukkit.block.BlockState;

public class BlockStateSnapshotResult {
    private final boolean isSnapshot;
    private final BlockState state;

    public BlockStateSnapshotResult(boolean isSnapshot, BlockState state) {
        this.isSnapshot = isSnapshot;
        this.state = state;
    }

    public boolean isSnapshot() {
        return this.isSnapshot;
    }

    public BlockState getState() {
        return this.state;
    }
}

