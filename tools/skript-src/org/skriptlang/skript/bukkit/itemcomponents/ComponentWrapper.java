/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.datacomponent.DataComponentBuilder
 *  io.papermc.paper.datacomponent.DataComponentType$Valued
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.itemcomponents;

import ch.njol.skript.aliases.ItemData;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.util.ItemSource;
import ch.njol.skript.util.slot.Slot;
import io.papermc.paper.datacomponent.DataComponentBuilder;
import io.papermc.paper.datacomponent.DataComponentType;
import java.util.function.Consumer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ComponentWrapper<T, B extends DataComponentBuilder<T>>
implements Cloneable {
    @Nullable
    private final ItemSource<?> itemSource;
    private T component;

    public ComponentWrapper(ItemStack itemStack) {
        this(new ItemSource<ItemStack>(itemStack));
    }

    public ComponentWrapper(ItemSource<?> itemSource) {
        if (itemSource.getItemStack() == null) {
            throw new IllegalArgumentException("ItemSource must have an ItemStack to retrieve");
        }
        this.itemSource = itemSource;
        this.component = this.getComponent(itemSource.getItemStack());
    }

    public ComponentWrapper(T component) {
        this.component = component;
        this.itemSource = null;
    }

    public ComponentWrapper(B builder) {
        this.component = builder.build();
        this.itemSource = null;
    }

    public T getComponent() {
        if (this.itemSource != null) {
            return this.getComponent(this.itemSource.getItemStack());
        }
        return this.component;
    }

    public B getBuilder() {
        if (this.itemSource != null) {
            return this.getBuilder((T)this.itemSource.getItemStack());
        }
        return this.getBuilder(this.component);
    }

    @Nullable
    public ItemStack getItemStack() {
        return this.itemSource == null ? null : this.itemSource.getItemStack();
    }

    @Nullable
    public ItemSource<?> getItemSource() {
        return this.itemSource;
    }

    public abstract DataComponentType.Valued<T> getDataComponentType();

    protected abstract T getComponent(ItemStack var1);

    protected abstract B getBuilder(ItemStack var1);

    protected abstract void setComponent(ItemStack var1, T var2);

    protected void setBuilder(ItemStack itemStack, B builder) {
        this.setComponent(itemStack, builder.build());
    }

    public void applyComponent() {
        this.applyComponent(this.getComponent());
    }

    public void applyComponent(@NotNull T component) {
        this.component = component;
        if (this.itemSource == null) {
            return;
        }
        ItemStack itemStack = this.itemSource.getItemStack();
        this.setComponent(itemStack, component);
        Object object = this.itemSource.getSource();
        if (object instanceof ItemType) {
            ItemType itemType = (ItemType)object;
            for (ItemData itemData : itemType) {
                ItemStack dataStack = itemData.getStack();
                if (dataStack == null) continue;
                dataStack.setData(this.getDataComponentType(), component);
            }
        } else {
            object = this.itemSource.getSource();
            if (object instanceof Slot) {
                Slot slot = (Slot)object;
                slot.setItem(itemStack);
            }
        }
    }

    public void applyBuilder(@NotNull B builder) {
        this.applyComponent(builder.build());
    }

    public void editBuilder(Consumer<B> consumer) {
        B builder = this.getBuilder();
        consumer.accept(builder);
        this.applyComponent(builder.build());
    }

    protected abstract B getBuilder(T var1);

    public abstract ComponentWrapper<T, B> clone();

    public abstract T newComponent();

    public abstract B newBuilder();

    public abstract ComponentWrapper<T, B> newWrapper();

    public boolean equals(Object obj) {
        if (!(obj instanceof ComponentWrapper)) {
            return false;
        }
        ComponentWrapper other = (ComponentWrapper)obj;
        boolean relation = true;
        if (this.itemSource != null && other.itemSource != null) {
            relation = this.itemSource.getItemStack().equals((Object)other.itemSource.getItemStack());
        }
        return relation &= this.getComponent().equals(other.getComponent());
    }

    public String toString() {
        return this.getComponent().toString();
    }
}

