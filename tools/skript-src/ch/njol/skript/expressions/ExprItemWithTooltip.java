/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Item with Tooltip")
@Description(value={"Get an item with or without entire/additional tooltip.", "If changing the 'entire' tooltip of an item, nothing will show up when a player hovers over it.", "If changing the 'additional' tooltip, only specific parts (which change per item) will be hidden."})
@Example.Examples(value={@Example(value="set {_item with additional tooltip} to diamond with additional tooltip"), @Example(value="set {_item without entire tooltip} to diamond without entire tooltip")})
@RequiredPlugins(value={"Minecraft 1.20.5+"})
@Since(value={"2.11"})
public class ExprItemWithTooltip
extends PropertyExpression<ItemType, ItemType> {
    private boolean without;
    private boolean entire;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(expressions[0]);
        this.without = parseResult.hasTag("out");
        this.entire = !parseResult.hasTag("additional");
        return true;
    }

    protected ItemType[] get(Event event, ItemType[] source) {
        return this.get(source, itemType -> {
            itemType = itemType.clone();
            ItemMeta meta = itemType.getItemMeta();
            if (this.entire) {
                meta.setHideTooltip(this.without);
            } else if (this.without) {
                meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
            } else {
                meta.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_ADDITIONAL_TOOLTIP});
            }
            itemType.setItemMeta(meta);
            return itemType;
        });
    }

    @Override
    public Class<? extends ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.getExpr().toString(event, debug) + (this.without ? " without" : " with") + (this.entire ? " entire" : " additional") + " tooltip";
    }

    static {
        if (Skript.methodExists(ItemMeta.class, "isHideTooltip", new Class[0])) {
            Skript.registerExpression(ExprItemWithTooltip.class, ItemType.class, ExpressionType.PROPERTY, "%itemtypes% with[:out] [entire|:additional] tool[ ]tip[s]");
        }
    }
}

