/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.meta.ItemMeta
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import org.bukkit.inventory.meta.ItemMeta;

@Name(value="Is Fire Resistant")
@Description(value={"Checks whether an item is fire resistant."})
@Example.Examples(value={@Example(value="if player's tool is fire resistant:"), @Example(value="if {_items::*} aren't resistant to fire:")})
@RequiredPlugins(value={"Spigot 1.20.5+"})
@Since(value={"2.9.0"})
public class CondIsFireResistant
extends PropertyCondition<ItemType> {
    @Override
    public boolean check(ItemType item) {
        return item.getItemMeta().isFireResistant();
    }

    @Override
    public String getPropertyName() {
        return "fire resistant";
    }

    static {
        if (Skript.methodExists(ItemMeta.class, "isFireResistant", new Class[0])) {
            PropertyCondition.register(CondIsFireResistant.class, "(fire resistant|resistant to fire)", "itemtypes");
        }
    }
}

