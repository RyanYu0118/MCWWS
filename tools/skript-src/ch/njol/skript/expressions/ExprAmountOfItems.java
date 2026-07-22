/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Amount of Items")
@Description(value={"Counts how many of a particular <a href='#itemtype'>item type</a> are in a given inventory."})
@Example(value="message \"You have %number of tag values of minecraft tag \"diamond_ores\" in the player's inventory% diamond ores in your inventory.\"")
@Since(value={"2.0"})
public class ExprAmountOfItems
extends SimpleExpression<Long> {
    private Expression<ItemType> items;
    private Expression<Inventory> inventories;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.items = exprs[0];
        this.inventories = exprs[1];
        return true;
    }

    protected Long[] get(Event e) {
        ItemType[] itemTypes = this.items.getArray(e);
        long amount = 0L;
        for (Inventory inventory : this.inventories.getArray(e)) {
            block1: for (ItemStack itemStack : inventory.getContents()) {
                if (itemStack == null) continue;
                for (ItemType itemType : itemTypes) {
                    if (!new ItemType(itemStack).isSimilar(itemType)) continue;
                    amount += (long)itemStack.getAmount();
                    continue block1;
                }
            }
        }
        return new Long[]{amount};
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the number of " + this.items.toString(e, debug) + " in " + this.inventories.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprAmountOfItems.class, Long.class, ExpressionType.PROPERTY, "[the] (amount|number) of %itemtypes% (in|of) %inventories%");
    }
}

