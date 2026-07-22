/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.Inventory
 */
package ch.njol.skript.paperlib.features.inventoryholdersnapshot;

import ch.njol.skript.paperlib.features.inventoryholdersnapshot.InventoryHolderSnapshot;
import ch.njol.skript.paperlib.features.inventoryholdersnapshot.InventoryHolderSnapshotResult;
import org.bukkit.inventory.Inventory;

public class InventoryHolderSnapshotNoOption
implements InventoryHolderSnapshot {
    @Override
    public InventoryHolderSnapshotResult getHolder(Inventory inventory, boolean useSnapshot) {
        return new InventoryHolderSnapshotResult(true, inventory.getHolder());
    }
}

