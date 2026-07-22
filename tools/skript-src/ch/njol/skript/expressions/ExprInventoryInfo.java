/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import java.util.ArrayList;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;

@Name(value="Inventory Holder/Viewers/Rows/Slots")
@Description(value={"Gets the amount of rows/slots, viewers and holder of an inventory.", "", "NOTE: 'Viewers' expression returns a list of players viewing the inventory. Note that a player is considered to be viewing their own inventory and internal crafting screen even when said inventory is not open."})
@Example.Examples(value={@Example(value="event-inventory's amount of rows"), @Example(value="holder of player's top inventory"), @Example(value="{_inventory}'s viewers")})
@Since(value={"2.2-dev34, 2.5 (slots)"})
public class ExprInventoryInfo
extends SimpleExpression<Object> {
    private static final int HOLDER = 1;
    private static final int VIEWERS = 2;
    private static final int ROWS = 3;
    private static final int SLOTS = 4;
    private Expression<Inventory> inventories;
    private int type;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.inventories = exprs[0];
        this.type = parseResult.mark;
        return true;
    }

    @Override
    protected Object[] get(Event event) {
        Inventory[] inventories = this.inventories.getArray(event);
        switch (this.type) {
            case 1: {
                ArrayList<InventoryHolder> holders = new ArrayList<InventoryHolder>();
                for (Inventory inventory : inventories) {
                    InventoryHolder holder = inventory.getHolder();
                    if (holder == null) continue;
                    holders.add(holder);
                }
                return holders.toArray(new InventoryHolder[0]);
            }
            case 3: {
                ArrayList<Integer> rows = new ArrayList<Integer>();
                for (Inventory inventory : inventories) {
                    int size = inventory.getSize();
                    if (size < 9) {
                        rows.add(1);
                        continue;
                    }
                    rows.add(size / 9);
                }
                return rows.toArray(new Number[0]);
            }
            case 4: {
                ArrayList<Integer> sizes = new ArrayList<Integer>();
                for (Inventory inventory : inventories) {
                    sizes.add(inventory.getSize());
                }
                return sizes.toArray(new Number[0]);
            }
            case 2: {
                ArrayList viewers = new ArrayList();
                for (Inventory inventory : inventories) {
                    viewers.addAll(inventory.getViewers());
                }
                return viewers.stream().filter(viewer -> viewer instanceof Player).toArray(Player[]::new);
            }
        }
        return (Object[])Array.newInstance(this.getReturnType(), 0);
    }

    @Override
    public boolean isSingle() {
        return this.inventories.isSingle() && this.type != 2;
    }

    @Override
    public Class<?> getReturnType() {
        return this.type == 1 ? InventoryHolder.class : (this.type == 3 || this.type == 4 ? Number.class : Player.class);
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return (this.type == 1 ? "holder of " : (this.type == 3 ? "rows of " : (this.type == 4 ? "slots of " : "viewers of "))) + this.inventories.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprInventoryInfo.class, Object.class, ExpressionType.PROPERTY, "(1\u00a6holder[s]|2\u00a6viewers|3\u00a6[amount of] rows|4\u00a6[amount of] slots) of %inventories%", "%inventories%'[s] (1\u00a6holder[s]|2\u00a6viewers|3\u00a6[amount of] rows|4\u00a6[amount of] slots)");
    }
}

