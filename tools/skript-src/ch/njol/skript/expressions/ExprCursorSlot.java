/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.slot.CursorSlot;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Cursor Slot")
@Description(value={"The item which the player has on their inventory cursor. This slot is always empty if player has no inventory open."})
@Example.Examples(value={@Example(value="cursor slot of player is dirt"), @Example(value="set cursor slot of player to 64 diamonds")})
@Since(value={"2.2-dev17"})
public class ExprCursorSlot
extends PropertyExpression<Player, Slot> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        return true;
    }

    protected Slot[] get(Event event, Player[] source) {
        return this.get(source, player -> {
            if (event instanceof InventoryClickEvent) {
                return new CursorSlot((Player)player, ((InventoryClickEvent)event).getCursor());
            }
            return new CursorSlot((Player)player);
        });
    }

    @Override
    public Class<? extends Slot> getReturnType() {
        return Slot.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "cursor slot of " + this.getExpr().toString(event, debug);
    }

    static {
        ExprCursorSlot.register(ExprCursorSlot.class, Slot.class, "cursor slot", "players");
    }
}

