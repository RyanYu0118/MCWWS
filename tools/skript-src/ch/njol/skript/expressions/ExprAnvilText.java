/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.AnvilInventory
 *  org.bukkit.inventory.Inventory
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

@Name(value="Anvil Text Input")
@Description(value={"An expression to get the name to be applied to an item in an anvil inventory."})
@Example(value="on inventory click:\n\ttype of event-inventory is anvil inventory\n\tif the anvil text input of the event-inventory is \"FREE OP\":\n\t\tban player\n")
@Since(value={"2.7"})
public class ExprAnvilText
extends SimplePropertyExpression<Inventory, String> {
    @Override
    @Nullable
    public String convert(Inventory inv) {
        if (!(inv instanceof AnvilInventory)) {
            return null;
        }
        return ((AnvilInventory)inv).getRenameText();
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String getPropertyName() {
        return "anvil text input";
    }

    static {
        ExprAnvilText.register(ExprAnvilText.class, String.class, "anvil [inventory] (rename|text) input", "inventories");
    }
}

