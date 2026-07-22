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
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.slot.InventorySlot;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Items In")
@Description(value={"All items or specific type(s) of items in an inventory. Useful for looping or storing in a list variable.", "Please note that the positions of the items in the inventory are not saved, only their order is preserved."})
@Example.Examples(value={@Example(value="loop all items in the player's inventory:\n\tloop-item is enchanted\n\tremove loop-item from the player\n"), @Example(value="set {inventory::%uuid of player%::*} to items in the player's inventory")})
@Since(value={"2.0, 2.8.0 (specific types of items)"})
public class ExprItemsIn
extends SimpleExpression<Slot> {
    private Expression<Inventory> inventories;
    @Nullable
    private Expression<ItemType> types;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0) {
            this.inventories = exprs[0];
        } else {
            this.types = exprs[0];
            this.inventories = exprs[1];
        }
        if (this.inventories instanceof Variable && !this.inventories.isSingle() && parseResult.mark != 1) {
            Skript.warning("'items in {variable::*}' does not actually represent the items stored in the variable. Use either '{variable::*}' (e.g. 'loop {variable::*}') if the variable contains items, or 'items in inventories {variable::*}' if the variable contains inventories.");
        }
        return true;
    }

    protected Slot[] get(Event event) {
        ArrayList<InventorySlot> itemSlots = new ArrayList<InventorySlot>();
        ItemType[] types = this.types == null ? null : this.types.getArray(event);
        for (Inventory inventory : this.inventories.getArray(event)) {
            for (int i = 0; i < inventory.getSize(); ++i) {
                if (!this.isAllowedItem(types, inventory.getItem(i))) continue;
                itemSlots.add(new InventorySlot(inventory, i));
            }
        }
        return itemSlots.toArray(new Slot[itemSlots.size()]);
    }

    @Override
    @Nullable
    public Iterator<Slot> iterator(Event event) {
        ItemType[] types;
        final Iterator inventoryIterator = this.inventories.iterator(event);
        ItemType[] itemTypeArray = types = this.types == null ? null : this.types.getArray(event);
        if (inventoryIterator == null || !inventoryIterator.hasNext()) {
            return null;
        }
        return new Iterator<Slot>(){
            Inventory currentInventory;
            int next;
            final /* synthetic */ ExprItemsIn this$0;
            {
                this.this$0 = this$0;
                this.currentInventory = (Inventory)inventoryIterator.next();
                this.next = 0;
            }

            @Override
            public boolean hasNext() {
                while (this.next < this.currentInventory.getSize() && !this.this$0.isAllowedItem(types, this.currentInventory.getItem(this.next))) {
                    ++this.next;
                }
                while (this.next >= this.currentInventory.getSize() && inventoryIterator.hasNext()) {
                    this.currentInventory = (Inventory)inventoryIterator.next();
                    this.next = 0;
                    while (this.next < this.currentInventory.getSize() && !this.this$0.isAllowedItem(types, this.currentInventory.getItem(this.next))) {
                        ++this.next;
                    }
                }
                return this.next < this.currentInventory.getSize();
            }

            @Override
            public Slot next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                return new InventorySlot(this.currentInventory, this.next++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean isLoopOf(String s) {
        return s.equalsIgnoreCase("item");
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.types == null) {
            return "items in " + this.inventories.toString(event, debug);
        }
        return "all " + this.types.toString(event, debug) + " in " + this.inventories.toString(event, debug);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<Slot> getReturnType() {
        return Slot.class;
    }

    private boolean isAllowedItem(@Nullable ItemType[] types, @Nullable ItemStack item) {
        if (types == null) {
            return item != null;
        }
        if (item == null) {
            return false;
        }
        ItemType potentiallyAllowedItem = new ItemType(item);
        for (ItemType type : types) {
            if (!potentiallyAllowedItem.isSimilar(type)) continue;
            return true;
        }
        return false;
    }

    static {
        Skript.registerExpression(ExprItemsIn.class, Slot.class, ExpressionType.PROPERTY, "[all [[of] the]] items ([with]in|of|contained in|out of) [1:inventor(y|ies)] %inventories%", "all [[of] the] %itemtypes% ([with]in|of|contained in|out of) [1:inventor(y|ies)] %inventories%");
    }
}

