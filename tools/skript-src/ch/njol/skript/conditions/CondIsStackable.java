/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.ItemStack
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.inventory.ItemStack;

@Name(value="Is Stackable")
@Description(value={"Checks whether an item is stackable."})
@Example.Examples(value={@Example(value="diamond axe is stackable"), @Example(value="birch wood is stackable"), @Example(value="torch is stackable")})
@Since(value={"2.7"})
public class CondIsStackable
extends PropertyCondition<ItemStack> {
    @Override
    public boolean check(ItemStack item) {
        return item.getMaxStackSize() > 1;
    }

    @Override
    protected String getPropertyName() {
        return "stackable";
    }

    static {
        CondIsStackable.register(CondIsStackable.class, "stackable", "itemstacks");
    }
}

