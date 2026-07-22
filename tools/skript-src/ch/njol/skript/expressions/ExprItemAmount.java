/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Item Amount")
@Description(value={"The amount of an <a href='#itemstack'>item stack</a>."})
@Example(value="send \"You have got %item amount of player's tool% %player's tool% in your hand!\" to player")
@Since(value={"2.2-dev24"})
public class ExprItemAmount
extends SimplePropertyExpression<Object, Long> {
    @Override
    public Long convert(Object item) {
        if (item instanceof ItemType) {
            return ((ItemType)item).getAmount();
        }
        if (item instanceof Slot) {
            return ((Slot)item).getAmount();
        }
        return ((ItemStack)item).getAmount();
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET: 
            case ADD: 
            case RESET: 
            case DELETE: 
            case REMOVE: {
                return CollectionUtils.array(Long.class);
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        int amount = delta != null ? ((Number)delta[0]).intValue() : 0;
        switch (mode) {
            case REMOVE: {
                amount = -amount;
            }
            case ADD: {
                for (Object obj : this.getExpr().getArray(event)) {
                    ItemType item;
                    if (obj instanceof ItemType) {
                        item = (ItemType)obj;
                        item.setAmount(item.getAmount() + amount);
                        continue;
                    }
                    if (obj instanceof Slot) {
                        Slot slot = (Slot)obj;
                        slot.setAmount(slot.getAmount() + amount);
                        continue;
                    }
                    item = (ItemStack)obj;
                    item.setAmount(item.getAmount() + amount);
                }
                break;
            }
            case RESET: 
            case DELETE: {
                amount = 1;
            }
            case SET: {
                for (Object obj : this.getExpr().getArray(event)) {
                    if (obj instanceof ItemType) {
                        ((ItemType)obj).setAmount(amount);
                        continue;
                    }
                    if (obj instanceof Slot) {
                        ((Slot)obj).setAmount(amount);
                        continue;
                    }
                    ((ItemStack)obj).setAmount(amount);
                }
                break;
            }
        }
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    protected String getPropertyName() {
        return "item amount";
    }

    static {
        ExprItemAmount.register(ExprItemAmount.class, Long.class, "item[[ ]stack] (amount|size|number)", "slots/itemtypes/itemstacks");
    }
}

