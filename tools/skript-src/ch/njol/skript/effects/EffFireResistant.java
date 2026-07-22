/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Make Fire Resistant")
@Description(value={"Makes items fire resistant."})
@Example.Examples(value={@Example(value="make player's tool fire resistant"), @Example(value="make {_items::*} not resistant to fire")})
@RequiredPlugins(value={"Spigot 1.20.5+"})
@Since(value={"2.9.0"})
public class EffFireResistant
extends Effect {
    private Expression<ItemType> items;
    private boolean not;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.items = exprs[0];
        this.not = parseResult.hasTag("not");
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (ItemType item : this.items.getArray(event)) {
            ItemMeta meta = item.getItemMeta();
            meta.setFireResistant(!this.not);
            item.setItemMeta(meta);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "make " + this.items.toString(event, debug) + (this.not ? " not" : "") + " fire resistant";
    }

    static {
        if (Skript.methodExists(ItemMeta.class, "setFireResistant", Boolean.TYPE)) {
            Skript.registerEffect(EffFireResistant.class, "make %itemtypes% [:not] (fire resistant|resistant to fire)");
        }
    }
}

