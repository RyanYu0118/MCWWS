/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.block.BlockState
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.CraftingInventory
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util.slot;

import ch.njol.skript.bukkitutil.PlayerUtils;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.BlockInventoryHolder;
import ch.njol.skript.util.slot.Slot;
import ch.njol.skript.util.slot.SlotWithIndex;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class InventorySlot
extends SlotWithIndex {
    private final Inventory inventory;
    private final int index;
    private final int rawIndex;

    public InventorySlot(Inventory inventory, int index, int rawIndex) {
        assert (inventory != null);
        assert (index >= 0);
        this.inventory = inventory;
        this.index = index;
        this.rawIndex = rawIndex;
    }

    public InventorySlot(Inventory inventory, int index) {
        assert (inventory != null);
        assert (index >= 0);
        this.inventory = inventory;
        this.index = this.rawIndex = index;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public int getRawIndex() {
        return this.rawIndex;
    }

    @Override
    @Nullable
    public ItemStack getItem() {
        if (this.index == -999) {
            return null;
        }
        ItemStack item = this.inventory.getItem(this.index);
        return item == null ? new ItemStack(Material.AIR, 1) : item;
    }

    @Override
    public void setItem(@Nullable ItemStack item) {
        this.inventory.setItem(this.index, item != null && item.getType() != Material.AIR ? item : null);
        if (this.inventory instanceof PlayerInventory) {
            PlayerUtils.updateInventory((Player)this.inventory.getHolder());
        }
    }

    @Override
    public int getAmount() {
        ItemStack item = this.inventory.getItem(this.index);
        return item != null ? item.getAmount() : 0;
    }

    @Override
    public void setAmount(int amount) {
        ItemStack item = this.inventory.getItem(this.index);
        if (item != null) {
            item.setAmount(amount);
        }
        if (this.inventory instanceof PlayerInventory) {
            PlayerUtils.updateInventory((Player)this.inventory.getHolder());
        }
    }

    @Override
    public boolean isSameSlot(Slot slot) {
        if (slot instanceof InventorySlot) {
            InventorySlot inventorySlot = (InventorySlot)slot;
            return inventorySlot.getInventory().equals((Object)this.inventory) && inventorySlot.getIndex() == this.index;
        }
        return super.equals(slot);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        InventoryHolder holder = this.inventory.getHolder();
        if (holder instanceof BlockState) {
            holder = new BlockInventoryHolder((BlockState)holder);
        }
        if (holder != null) {
            if (this.inventory instanceof CraftingInventory) {
                return "crafting slot " + this.index + " of " + Classes.toString(holder);
            }
            return "inventory slot " + this.index + " of " + Classes.toString(holder);
        }
        return "inventory slot " + this.index + " of " + Classes.toString(this.inventory);
    }
}

