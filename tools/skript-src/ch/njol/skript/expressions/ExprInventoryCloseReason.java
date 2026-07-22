/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent$Reason
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.ExpressionType;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Inventory Close Reason")
@Description(value={"The <a href='/#inventoryclosereason'>inventory close reason</a> of an <a href='/#inventory_close'>inventory close event</a>."})
@Example(value="on inventory close:\n\tinventory close reason is teleport\n\tsend \"Your inventory closed due to teleporting!\" to player\n")
@Events(value={"Inventory Close"})
@Since(value={"2.8.0"})
public class ExprInventoryCloseReason
extends EventValueExpression<InventoryCloseEvent.Reason>
implements EventRestrictedSyntax {
    public ExprInventoryCloseReason() {
        super(InventoryCloseEvent.Reason.class);
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return new Class[]{InventoryCloseEvent.class};
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "inventory close reason";
    }

    static {
        Skript.registerExpression(ExprInventoryCloseReason.class, InventoryCloseEvent.Reason.class, ExpressionType.SIMPLE, "[the] inventory clos(e|ing) (reason|cause)");
    }
}

