/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.Inventory
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.InventoryUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

@Name(value="Opened Inventory")
@Description(value={"Return the currently opened inventory of a player.", "If no inventory is open, it returns the own player's crafting inventory."})
@Example(value="set slot 1 of player's current inventory to diamond sword")
@Since(value={"2.2-dev24, 2.2-dev35 (Just 'current inventory' works in player events)"})
public class ExprOpenedInventory
extends PropertyExpression<Player, Inventory> {
    @Override
    public Class<? extends Inventory> getReturnType() {
        return Inventory.class;
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.setExpr(exprs[0]);
        return true;
    }

    protected Inventory[] get(Event event, Player[] source) {
        return this.get(source, player -> InventoryUtils.getTopInventory(player.getOpenInventory()));
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "current inventory" + (String)(this.getExpr().isDefault() ? "" : " of " + this.getExpr().toString(event, debug));
    }

    static {
        Skript.registerExpression(ExprOpenedInventory.class, Inventory.class, ExpressionType.PROPERTY, "[the] (current|open|top) inventory [of %players%]", "%players%'[s] (current|open|top) inventory");
    }
}

