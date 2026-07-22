/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemFlag
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Item Tooltips")
@Description(value={"Show or hide the tooltip of an item.", "If changing the 'entire' tooltip of an item, nothing will show up when a player hovers over it.", "If changing the 'additional' tooltip, only specific parts (which change per item) will be hidden."})
@Example.Examples(value={@Example(value="hide the entire tooltip of player's tool"), @Example(value="hide {_item}'s additional tool tip")})
@RequiredPlugins(value={"Spigot 1.20.5+"})
@Since(value={"2.9.0"})
public class EffTooltip
extends Effect {
    private Expression<ItemType> items;
    private boolean hide;
    private boolean entire;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.items = exprs[0];
        this.hide = parseResult.hasTag("hide");
        this.entire = !parseResult.hasTag("additional");
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (ItemType item : this.items.getArray(event)) {
            ItemMeta meta = item.getItemMeta();
            if (this.entire) {
                meta.setHideTooltip(this.hide);
            } else if (this.hide) {
                meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
            } else {
                meta.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
            }
            item.setItemMeta(meta);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.hide ? "hide" : "show") + " the " + (this.entire ? "entire" : "additional") + " tooltip of " + this.items.toString(event, debug);
    }

    static {
        if (Skript.methodExists(ItemMeta.class, "setHideTooltip", Boolean.TYPE)) {
            Skript.registerEffect(EffTooltip.class, "(show|reveal|:hide) %itemtypes%'[s] [entire|:additional] tool[ ]tip", "(show|reveal|:hide) [the] [entire|:additional] tool[ ]tip of %itemtypes%");
        }
    }
}

