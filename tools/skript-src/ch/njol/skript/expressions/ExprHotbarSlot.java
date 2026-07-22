/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerItemHeldEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.PlayerInventory
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.slot.InventorySlot;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

@Name(value="Hotbar Slot")
@Description(value={"The currently selected hotbar <a href='#slot'>slot</a>.", "To retrieve its number use <a href='#ExprSlotIndex'>Slot Index</a> expression.", "Use future and past tense to grab the previous slot in an item change event, see example."})
@Example.Examples(value={@Example(value="message \"%player's current hotbar slot%\""), @Example(value="set player's selected hotbar slot to slot 4 of player"), @Example(value="send \"index of player's current hotbar slot = 1\" # second slot from the left"), @Example(value="on item held change:\n\tif the selected hotbar slot was a diamond:\n\t\tset the currently selected hotbar slot to slot 5 of player\n")})
@Since(value={"2.2-dev36"})
public class ExprHotbarSlot
extends PropertyExpression<Player, Slot> {
    private boolean current;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.current = parseResult.hasTag("current");
        return true;
    }

    protected Slot[] get(Event event, Player[] source) {
        return this.get(source, player -> {
            int time = this.getTime();
            PlayerInventory inventory = player.getInventory();
            if (event instanceof PlayerItemHeldEvent) {
                PlayerItemHeldEvent switchEvent = (PlayerItemHeldEvent)event;
                if (time != EventValues.TIME_NOW) {
                    if (time == EventValues.TIME_FUTURE) {
                        return new InventorySlot((Inventory)inventory, switchEvent.getNewSlot());
                    }
                    if (time == EventValues.TIME_PAST) {
                        return new InventorySlot((Inventory)inventory, switchEvent.getPreviousSlot());
                    }
                }
            }
            return new InventorySlot((Inventory)inventory, inventory.getHeldItemSlot());
        });
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return new Class[]{Slot.class, Number.class};
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        assert (delta != null);
        Object object = delta[0];
        Number number = null;
        if (object instanceof InventorySlot) {
            number = ((InventorySlot)object).getIndex();
        } else if (object instanceof Number) {
            number = (Number)object;
        }
        if (number == null) {
            return;
        }
        int index = number.intValue();
        if (index > 8 || index < 0) {
            return;
        }
        for (Player player : (Player[])this.getExpr().getArray(event)) {
            player.getInventory().setHeldItemSlot(index);
        }
    }

    @Override
    public boolean setTime(int time) {
        if (this.current) {
            return super.setTime(time);
        }
        return super.setTime(time, this.getExpr(), PlayerItemHeldEvent.class);
    }

    @Override
    public Class<? extends Slot> getReturnType() {
        return Slot.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "hotbar slot of " + this.getExpr().toString(event, debug);
    }

    static {
        ExprHotbarSlot.registerDefault(ExprHotbarSlot.class, Slot.class, "[([current:currently] selected|current:current)] hotbar slot[s]", "players");
    }
}

