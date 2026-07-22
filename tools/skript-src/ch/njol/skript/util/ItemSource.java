/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class ItemSource<T> {
    private final T source;

    public ItemSource(T source) {
        this.source = source;
    }

    @Nullable
    public static ItemSource<Slot> fromSlot(Slot slot) {
        ItemStack itemStack = slot.getItem();
        if (itemStack == null || ItemUtils.isAir(itemStack.getType()) || itemStack.getItemMeta() == null) {
            return null;
        }
        return new ItemSource<Slot>(slot);
    }

    public T getSource() {
        return this.source;
    }

    public ItemStack getItemStack() {
        return ItemUtils.asItemStack(this.source);
    }

    public ItemMeta getItemMeta() {
        return this.getItemStack().getItemMeta();
    }

    public void setItemMeta(ItemMeta itemMeta) {
        ItemUtils.setItemMeta(this.source, itemMeta);
    }
}

