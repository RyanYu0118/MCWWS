/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.ThrowableProjectile
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util.slot;

import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.Material;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ThrowableProjectileSlot
extends Slot {
    private final ThrowableProjectile projectile;

    public ThrowableProjectileSlot(ThrowableProjectile projectile) {
        this.projectile = projectile;
    }

    @Override
    public ItemStack getItem() {
        return this.projectile.getItem();
    }

    @Override
    public void setItem(@Nullable ItemStack item) {
        this.projectile.setItem(item != null ? item : new ItemStack(Material.AIR));
    }

    @Override
    public int getAmount() {
        return 1;
    }

    @Override
    public void setAmount(int amount) {
    }

    public ThrowableProjectile getProjectile() {
        return this.projectile;
    }

    @Override
    public boolean isSameSlot(Slot slot) {
        ThrowableProjectileSlot throwableSlot;
        return slot instanceof ThrowableProjectileSlot && (throwableSlot = (ThrowableProjectileSlot)slot).getProjectile().equals((Object)this.projectile);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return Classes.toString(this.getItem());
    }
}

