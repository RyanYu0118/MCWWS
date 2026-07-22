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
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Max Durability")
@Description(value={"The maximum durability of an item. Changing requires Minecraft 1.20.5+", "Note: 'delete' will remove the max durability from the item (making it a non-damageable item). Delete requires Paper 1.21+"})
@Example.Examples(value={@Example(value="maximum durability of diamond sword"), @Example(value="if max durability of player's tool is not 0: # Item is damageable"), @Example(value="set max durability of player's tool to 5000"), @Example(value="add 5 to max durability of player's tool"), @Example(value="reset max durability of player's tool"), @Example(value="delete max durability of player's tool")})
@RequiredPlugins(value={"Minecraft 1.20.5+ (custom amount)"})
@Since(value={"2.5, 2.9.0 (change)"})
public class ExprMaxDurability
extends SimplePropertyExpression<Object, Integer> {
    @Override
    @Nullable
    public Integer convert(Object object) {
        ItemStack itemStack = ItemUtils.asItemStack(object);
        if (itemStack == null) {
            return null;
        }
        return ItemUtils.getMaxDamage(itemStack);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (ItemUtils.HAS_MAX_DAMAGE) {
            switch (mode) {
                case SET: 
                case ADD: 
                case REMOVE: 
                case RESET: {
                    return CollectionUtils.array(Number.class);
                }
                case DELETE: {
                    if (!ItemUtils.HAS_RESET) break;
                    return CollectionUtils.array(new Class[0]);
                }
            }
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        int change;
        int n = change = delta == null ? 0 : ((Number)delta[0]).intValue();
        if (mode == Changer.ChangeMode.REMOVE) {
            change = -change;
        }
        for (Object object : this.getExpr().getArray(event)) {
            ItemStack itemStack = ItemUtils.asItemStack(object);
            if (itemStack == null) continue;
            ItemUtils.setMaxDamage(itemStack, switch (mode) {
                case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> ItemUtils.getMaxDamage(itemStack) + change;
                case Changer.ChangeMode.SET -> change;
                case Changer.ChangeMode.DELETE -> 0;
                default -> itemStack.getType().getMaxDurability();
            });
            if (object instanceof Slot) {
                ((Slot)object).setItem(itemStack);
            }
            if (!(object instanceof ItemType)) continue;
            ((ItemType)object).setItemMeta(itemStack.getItemMeta());
        }
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return "max durability";
    }

    static {
        ExprMaxDurability.register(ExprMaxDurability.class, Integer.class, "max[imum] (durabilit(y|ies)|damage)", "itemtypes/itemstacks/slots");
    }
}

