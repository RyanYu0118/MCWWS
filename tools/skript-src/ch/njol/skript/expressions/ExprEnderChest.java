/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

@Name(value="Ender Chest")
@Description(value={"The ender chest of a player."})
@Example(value="open the player's ender chest to the player")
@Since(value={"2.0"})
public class ExprEnderChest
extends SimplePropertyExpression<Player, Inventory> {
    @Override
    @Nullable
    public Inventory convert(Player p) {
        return p.getEnderChest();
    }

    @Override
    public Class<? extends Inventory> getReturnType() {
        return Inventory.class;
    }

    @Override
    protected String getPropertyName() {
        return "ender chest";
    }

    static {
        ExprEnderChest.register(ExprEnderChest.class, Inventory.class, "ender[ ]chest[s]", "players");
    }
}

