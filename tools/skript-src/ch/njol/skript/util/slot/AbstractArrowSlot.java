/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.AbstractArrow
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util.slot;

import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AbstractArrowSlot
extends Slot {
    private final AbstractArrow projectile;

    public AbstractArrowSlot(AbstractArrow projectile) {
        this.projectile = projectile;
    }

    @Override
    public ItemStack getItem() {
        return this.projectile.getItemStack();
    }

    @Override
    public void setItem(@Nullable ItemStack item) {
        this.projectile.setItemStack(item != null ? item : new ItemStack(Material.AIR));
    }

    @Override
    public int getAmount() {
        return this.projectile.getItemStack().getAmount();
    }

    @Override
    public void setAmount(int amount) {
        this.projectile.getItemStack().setAmount(amount);
    }

    public AbstractArrow getProjectile() {
        return this.projectile;
    }

    @Override
    public boolean isSameSlot(Slot slot) {
        AbstractArrowSlot arrowSlot;
        return slot instanceof AbstractArrowSlot && (arrowSlot = (AbstractArrowSlot)slot).getProjectile().equals((Object)this.projectile);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return Classes.toString(this.getItem());
    }
}

