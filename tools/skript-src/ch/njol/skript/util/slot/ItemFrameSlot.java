/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.ItemFrame
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util.slot;

import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemFrameSlot
extends Slot {
    private final ItemFrame frame;

    public ItemFrameSlot(ItemFrame frame) {
        this.frame = frame;
    }

    @Override
    @Nullable
    public ItemStack getItem() {
        return this.frame.getItem();
    }

    @Override
    public void setItem(@Nullable ItemStack item) {
        this.frame.setItem(item);
    }

    @Override
    public int getAmount() {
        return 1;
    }

    @Override
    public void setAmount(int amount) {
    }

    public ItemFrame getItemFrame() {
        return this.frame;
    }

    @Override
    public boolean isSameSlot(Slot slot) {
        ItemFrameSlot itemFrameSlot;
        return slot instanceof ItemFrameSlot && (itemFrameSlot = (ItemFrameSlot)slot).getItemFrame().equals((Object)this.frame);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return Classes.toString(this.getItem());
    }
}

