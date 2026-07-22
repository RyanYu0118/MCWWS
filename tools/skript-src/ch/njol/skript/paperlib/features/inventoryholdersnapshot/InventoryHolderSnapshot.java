/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.Inventory
 */
package ch.njol.skript.paperlib.features.inventoryholdersnapshot;

import ch.njol.skript.paperlib.features.inventoryholdersnapshot.InventoryHolderSnapshotResult;
import org.bukkit.inventory.Inventory;

public interface InventoryHolderSnapshot {
    public InventoryHolderSnapshotResult getHolder(Inventory var1, boolean var2);
}

