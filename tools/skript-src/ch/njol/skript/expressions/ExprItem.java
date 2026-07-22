/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Item
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Item")
@Description(value={"The item involved in an event, e.g. in a drop, dispense, pickup or craft event."})
@Example(value="on dispense:\n\titem is a clock\n\tset the time to 6:00\n")
@Since(value={"unknown (before 2.1)"})
public class ExprItem
extends EventValueExpression<ItemStack> {
    @Nullable
    private EventValueExpression<Item> item;
    @Nullable
    private EventValueExpression<Slot> slot;

    public ExprItem() {
        super(ItemStack.class);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.RESET) {
            return null;
        }
        this.item = new EventValueExpression<Item>(Item.class);
        if (this.item.init()) {
            return new Class[]{ItemType.class};
        }
        this.item = null;
        this.slot = new EventValueExpression<Slot>(Slot.class);
        if (this.slot.init()) {
            return new Class[]{ItemType.class};
        }
        this.slot = null;
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Slot slot;
        assert (mode != Changer.ChangeMode.RESET);
        ItemType itemType = delta == null ? null : (ItemType)delta[0];
        Item item = this.item != null ? (Item)this.item.getSingle(event) : null;
        Slot slot2 = slot = this.slot != null ? (Slot)this.slot.getSingle(event) : null;
        if (item == null && slot == null) {
            return;
        }
        ItemStack itemstack = item != null ? item.getItemStack() : (slot != null ? slot.getItem() : null);
        switch (mode) {
            case SET: {
                assert (itemType != null);
                itemstack = itemType.getRandom();
                break;
            }
            case ADD: 
            case REMOVE: 
            case REMOVE_ALL: {
                assert (itemType != null);
                if (!itemType.isOfType(itemstack)) break;
                if (mode == Changer.ChangeMode.ADD) {
                    itemstack = itemType.addTo(itemstack);
                    break;
                }
                if (mode == Changer.ChangeMode.REMOVE) {
                    itemstack = itemType.removeFrom(itemstack);
                    break;
                }
                itemstack = itemType.removeAll(itemstack);
                break;
            }
            case DELETE: {
                itemstack = null;
                if (item == null) break;
                item.remove();
                break;
            }
            case RESET: {
                assert (false);
                break;
            }
        }
        if (item != null && itemstack != null) {
            item.setItemStack(itemstack);
        } else if (slot != null) {
            slot.setItem(itemstack);
        } else assert (false);
    }

    static {
        ExprItem.register(ExprItem.class, ItemStack.class, "item");
    }
}

