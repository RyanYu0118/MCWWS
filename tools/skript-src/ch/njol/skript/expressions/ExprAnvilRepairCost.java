/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.AnvilInventory
 *  org.bukkit.inventory.Inventory
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

@Name(value="Anvil Repair Cost")
@Description(value={"Returns the experience cost (in levels) to complete the current repair or the maximum experience cost (in levels) to be allowed by the current repair.", "The default value of max cost set by vanilla Minecraft is 40."})
@Example.Examples(value={@Example(value="on inventory click:\n\tif {AnvilRepairSaleActive} = true:\n\t\twait a tick # recommended, to avoid client bugs\n\t\tset anvil repair cost to anvil repair cost * 50%\n\t\tsend \"Anvil repair sale is ON!\" to player\n"), @Example(value="on inventory click:\n\tplayer have permission \"anvil.repair.max.bypass\"\n\tset max repair cost of event-inventory to 99999\n")})
@Since(value={"2.8.0"})
public class ExprAnvilRepairCost
extends SimplePropertyExpression<Inventory, Integer> {
    private boolean isMax;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.isMax = parseResult.hasTag("max");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Integer convert(Inventory inventory) {
        if (!(inventory instanceof AnvilInventory)) {
            return null;
        }
        AnvilInventory anvilInventory = (AnvilInventory)inventory;
        return this.isMax ? anvilInventory.getMaximumRepairCost() : anvilInventory.getRepairCost();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.SET -> CollectionUtils.array(Number.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        int value = ((Number)delta[0]).intValue() * (mode == Changer.ChangeMode.REMOVE ? -1 : 1);
        for (Inventory inventory : (Inventory[])this.getExpr().getArray(event)) {
            if (!(inventory instanceof AnvilInventory)) continue;
            AnvilInventory anvilInventory = (AnvilInventory)inventory;
            int change = mode == Changer.ChangeMode.SET ? 0 : (this.isMax ? anvilInventory.getMaximumRepairCost() : anvilInventory.getRepairCost());
            int newValue = Math.max(change + value, 0);
            if (this.isMax) {
                anvilInventory.setMaximumRepairCost(newValue);
                continue;
            }
            anvilInventory.setRepairCost(newValue);
        }
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    public String getPropertyName() {
        return "anvil item" + (this.isMax ? " max" : "") + " repair cost";
    }

    static {
        ExprAnvilRepairCost.registerDefault(ExprAnvilRepairCost.class, Integer.class, "[anvil] [item] [:max[imum]] repair cost", "inventories");
    }
}

