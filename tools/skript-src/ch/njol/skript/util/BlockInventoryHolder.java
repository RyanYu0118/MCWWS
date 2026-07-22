/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.BlockState
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 */
package ch.njol.skript.util;

import ch.njol.skript.util.BlockStateBlock;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BlockInventoryHolder
extends BlockStateBlock
implements InventoryHolder {
    public BlockInventoryHolder(BlockState state) {
        super(state, false);
    }

    public Inventory getInventory() {
        return ((InventoryHolder)this.state).getInventory();
    }
}

