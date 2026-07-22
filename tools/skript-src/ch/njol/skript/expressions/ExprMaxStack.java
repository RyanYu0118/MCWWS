/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.event.Event
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Maximum Stack Size")
@Description(value={"The maximum stack size of an item (e.g. 64 for torches, 16 for buckets, 1 for swords, etc.) or inventory.", "In 1.20.5+, the maximum stack size of items can be changed to any integer from 1 to 99, and stacked up to the maximum stack size of the inventory they're in."})
@Example.Examples(value={@Example(value="send \"You can hold %max stack size of player's tool% of %type of player's tool% in a slot.\" to player"), @Example(value="set the maximum stack size of inventory of all players to 16"), @Example(value="add 8 to the maximum stack size of player's tool"), @Example(value="reset the maximum stack size of {_gui}")})
@Since(value={"2.1, 2.10 (changeable, inventories)"})
@RequiredPlugins(value={"Spigot 1.20.5+ (changeable)"})
public class ExprMaxStack
extends SimplePropertyExpression<Object, Integer> {
    private static final boolean CHANGEABLE_ITEM_STACK_SIZE;

    @Override
    @Nullable
    public Integer convert(Object from) {
        if (from instanceof ItemType) {
            ItemType itemType = (ItemType)from;
            return ExprMaxStack.getMaxStackSize(itemType);
        }
        if (from instanceof Inventory) {
            Inventory inventory = (Inventory)from;
            return inventory.getMaxStackSize();
        }
        return null;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.RESET, Changer.ChangeMode.SET -> {
                if (!CHANGEABLE_ITEM_STACK_SIZE && ItemType.class.isAssignableFrom(this.getExpr().getReturnType())) {
                    Skript.error("Changing the maximum stack size of items requires Minecraft 1.20.5 or newer!");
                    yield null;
                }
                yield CollectionUtils.array(Integer.class);
            }
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        int change = delta == null ? 0 : ((Number)delta[0]).intValue();
        for (Object source : this.getExpr().getArray(event)) {
            int size;
            if (source instanceof ItemType) {
                ItemType itemType = (ItemType)source;
                if (!CHANGEABLE_ITEM_STACK_SIZE) continue;
                size = ExprMaxStack.getMaxStackSize(itemType);
                switch (mode) {
                    case ADD: {
                        size += change;
                        break;
                    }
                    case SET: {
                        size = change;
                        break;
                    }
                    case REMOVE: {
                        size -= change;
                    }
                }
                ItemMeta meta = itemType.getItemMeta();
                meta.setMaxStackSize(mode != Changer.ChangeMode.RESET ? Integer.valueOf(Math2.fit(1, size, 99)) : null);
                itemType.setItemMeta(meta);
                continue;
            }
            if (!(source instanceof Inventory)) continue;
            Inventory inventory = (Inventory)source;
            size = inventory.getMaxStackSize();
            switch (mode) {
                case ADD: {
                    size += change;
                    break;
                }
                case SET: {
                    size = change;
                    break;
                }
                case REMOVE: {
                    size -= change;
                    break;
                }
                case RESET: {
                    size = Bukkit.createInventory(null, (InventoryType)inventory.getType()).getMaxStackSize();
                }
            }
            inventory.setMaxStackSize(size);
        }
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return "maximum stack size";
    }

    private static int getMaxStackSize(ItemType itemType) {
        Object item = itemType.getRandomStackOrMaterial();
        if (item instanceof ItemStack) {
            ItemStack stack = (ItemStack)item;
            return stack.getMaxStackSize();
        }
        if (item instanceof Material) {
            Material material = (Material)item;
            return material.getMaxStackSize();
        }
        throw new UnsupportedOperationException();
    }

    static {
        ExprMaxStack.register(ExprMaxStack.class, Integer.class, "max[imum] stack[[ ]size]", "itemtypes/inventories");
        CHANGEABLE_ITEM_STACK_SIZE = Skript.methodExists(ItemMeta.class, "setMaxStackSize", Integer.class);
    }
}

