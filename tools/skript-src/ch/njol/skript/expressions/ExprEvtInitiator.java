/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.inventory.InventoryMoveItemEvent
 *  org.bukkit.inventory.Inventory
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

@Name(value="Initiator Inventory")
@Description(value={"Returns the initiator inventory in an on <a href=\"?search=#inventory_item_move\">inventory item move</a> event."})
@Example(value="on inventory item move:\n\tholder of event-initiator-inventory is a chest\n\tbroadcast \"Item transport happening at %location at holder of event-initiator-inventory%!\"\n")
@Events(value={"Inventory Item Move"})
@Since(value={"2.8.0"})
public class ExprEvtInitiator
extends SimpleExpression<Inventory>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(InventoryMoveItemEvent.class);
    }

    protected Inventory[] get(Event event) {
        if (!(event instanceof InventoryMoveItemEvent)) {
            return new Inventory[0];
        }
        return CollectionUtils.array(((InventoryMoveItemEvent)event).getInitiator());
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Inventory> getReturnType() {
        return Inventory.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "event-initiator-inventory";
    }

    static {
        Skript.registerExpression(ExprEvtInitiator.class, Inventory.class, ExpressionType.SIMPLE, "[the] [event-]initiator[( |-)inventory]");
    }
}

