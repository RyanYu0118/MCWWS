/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.util.slot;

import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.lang.Debuggable;
import ch.njol.skript.lang.util.common.AnyAmount;
import ch.njol.skript.lang.util.common.AnyNamed;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public abstract class Slot
implements Debuggable,
AnyNamed,
AnyAmount {
    protected Slot() {
    }

    @Nullable
    public abstract ItemStack getItem();

    public abstract void setItem(@Nullable ItemStack var1);

    public abstract int getAmount();

    public abstract void setAmount(int var1);

    @Override
    public final String toString() {
        return this.toString(null, false);
    }

    public abstract boolean isSameSlot(Slot var1);

    @Override
    public @UnknownNullability String name() {
        ItemStack stack = this.getItem();
        if (stack != null && stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            return meta.hasDisplayName() ? meta.getDisplayName() : null;
        }
        return null;
    }

    @Override
    public @UnknownNullability Component nameComponent() {
        ItemStack stack = this.getItem();
        if (stack != null && stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            return meta.hasDisplayName() ? meta.displayName() : null;
        }
        return null;
    }

    @Override
    public boolean supportsNameChange() {
        return true;
    }

    @Override
    public void setName(String name) {
        ItemStack stack = this.getItem();
        if (stack != null && !ItemUtils.isAir(stack.getType())) {
            ItemMeta meta = stack.hasItemMeta() ? stack.getItemMeta() : Bukkit.getItemFactory().getItemMeta(stack.getType());
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
            this.setItem(stack);
        }
    }

    @Override
    public void setName(Component name) {
        ItemStack stack = this.getItem();
        if (stack != null && !ItemUtils.isAir(stack.getType())) {
            ItemMeta meta = stack.hasItemMeta() ? stack.getItemMeta() : Bukkit.getItemFactory().getItemMeta(stack.getType());
            meta.displayName(name);
            stack.setItemMeta(meta);
            this.setItem(stack);
        }
    }

    @Override
    @NotNull
    public Number amount() {
        return this.getAmount();
    }

    @Override
    public boolean supportsAmountChange() {
        return true;
    }

    @Override
    public void setAmount(@Nullable Number amount) {
        this.setAmount(amount != null ? amount.intValue() : 0);
    }
}

