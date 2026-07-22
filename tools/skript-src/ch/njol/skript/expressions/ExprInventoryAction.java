/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.inventory.InventoryAction
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import org.bukkit.event.inventory.InventoryAction;

@Name(value="Inventory Action")
@Description(value={"The <a href='#inventoryaction'>inventory action</a> of an inventory event. Please click on the link for more information."})
@Example(value="inventory action is pickup all")
@Since(value={"2.2-dev16"})
public class ExprInventoryAction
extends EventValueExpression<InventoryAction> {
    public ExprInventoryAction() {
        super(InventoryAction.class);
    }

    static {
        ExprInventoryAction.register(ExprInventoryAction.class, InventoryAction.class, "inventory action");
    }
}

