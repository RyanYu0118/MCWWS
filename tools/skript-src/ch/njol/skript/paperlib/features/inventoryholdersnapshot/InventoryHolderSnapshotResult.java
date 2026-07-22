/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.InventoryHolder
 */
package ch.njol.skript.paperlib.features.inventoryholdersnapshot;

import org.bukkit.inventory.InventoryHolder;

public class InventoryHolderSnapshotResult {
    private final boolean isSnapshot;
    private final InventoryHolder holder;

    public InventoryHolderSnapshotResult(boolean isSnapshot, InventoryHolder holder) {
        this.isSnapshot = isSnapshot;
        this.holder = holder;
    }

    public boolean isSnapshot() {
        return this.isSnapshot;
    }

    public InventoryHolder getHolder() {
        return this.holder;
    }
}

