/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util.slot;

import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DisplayEntitySlot
extends Slot {
    private final ItemDisplay display;

    public DisplayEntitySlot(ItemDisplay display) {
        this.display = display;
    }

    @Override
    @Nullable
    public ItemStack getItem() {
        return this.display.getItemStack();
    }

    @Override
    public void setItem(@Nullable ItemStack item) {
        this.display.setItemStack(item);
    }

    @Override
    public int getAmount() {
        return 1;
    }

    @Override
    public void setAmount(int amount) {
    }

    public ItemDisplay getItemDisplay() {
        return this.display;
    }

    @Override
    public boolean isSameSlot(Slot slot) {
        DisplayEntitySlot displayEntitySlot;
        return slot instanceof DisplayEntitySlot && (displayEntitySlot = (DisplayEntitySlot)slot).getItemDisplay().equals((Object)this.display);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return Classes.toString(this.getItem());
    }
}

